package org.xiaobai.prettylike.manager.cache;

import cn.hutool.core.util.HashUtil;
import lombok.Data;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 热key 检测器
 */
public class HeavyKeeper implements TopK{
    private static final int LOOKUP_TABLE_SIZE = 256;// 初始化查找表的大小
    private final int k;// 前 K 个热key

    /**
     * 用于定义桶的宽度和深度。
     */
    private final int width;
    private final int depth;

    private final double[] lookupTable; // 用于快速计算衰减因子的查找表
    private final Bucket[][] buckets;// 用于存储桶的二维数组(哈希表)
    private final PriorityQueue<Node> minHeap;// 按照 Node 的count字段升序的最小堆
    private final BlockingQueue<Item> expelledQueue; // 用于存储被挤出的元素
    private final Random random;
    private long total;// 记录的总的计数
    private final int minCount;// 最小计数阀值

    public HeavyKeeper(int k,int width,int depth,double decay,int minCount){
        this.k = k;
        this.width = width;
        this.depth = depth;
        this.minCount = minCount;

        this.lookupTable = new double[LOOKUP_TABLE_SIZE];
        for(int i = 0; i < LOOKUP_TABLE_SIZE; i++){
            lookupTable[i] = Math.pow(decay,i);
        }

        this.buckets = new Bucket[depth][width];
        for(int i = 0; i < depth; i++){
            for(int j = 0; j < width; j++){
                buckets[i][j] = new Bucket();
            }
        }

        this.minHeap = new PriorityQueue<>(Comparator.comparingInt(n -> n.count));
        this.expelledQueue = new LinkedBlockingQueue<>(k);
        this.random = new Random();
        this.total = 0;
    }

    @Override
    public AddResult add(String key, int increment) {
        // 对key取hash值 得到指纹
        byte[] keyBytes = key.getBytes();
        int itemFingerprint = hash(keyBytes);
        int maxCount = 0;

        // 遍历每个桶，更新桶的计数
        for(int i = 0; i < depth; i++){
            // 获取到桶的位置
            int bucketNumber = Math.abs(hash(keyBytes)) % width;
            Bucket bucket = buckets[i][bucketNumber];

            // 加锁 保证线程安全
            synchronized (bucket) {
                // 1.若桶的计数为 0，将当前键的指纹存入桶，并将计数设为 increment。
                // 2.若桶的指纹与当前键的指纹相同，将桶的计数加上 increment。
                // 3.若桶的指纹与当前键的指纹不同，尝试对桶的计数进行衰减操作，衰减概率由 lookupTable 决定，若计数减为0，则将当前键存入该桶
                if(bucket.count == 0){
                    bucket.fingerprint = itemFingerprint;
                    bucket.count = increment;
                    maxCount = Math.max(maxCount, bucket.count);
                }else if(bucket.fingerprint == itemFingerprint){
                    bucket.count += increment;
                    maxCount = Math.max(maxCount, bucket.count);
                }else {
                    for(int j = 0;j < increment;j++){
                        double decay = bucket.count < LOOKUP_TABLE_SIZE ?
                                lookupTable[bucket.count] :
                                lookupTable[LOOKUP_TABLE_SIZE - 1];
                        if(random.nextDouble() < decay){
                            bucket.count--;
                            if(bucket.count == 0){
                                bucket.fingerprint = itemFingerprint;
                                bucket.count = 1;
                                maxCount = Math.max(maxCount, bucket.count);
                                break;
                            }
                        }
                    }
                }
            }
        }

        total += increment;

        if(maxCount < minCount){
            return new AddResult(null,false,null);
        }

        // 更新最小堆
        synchronized (minHeap) {
            // 判断是否是热key
            boolean isHot = false;
            String expelled = null;

            Optional<Node> existing = minHeap.stream()
                    .filter(n -> n.key.equals(key))
                    .findFirst();

            if (existing.isPresent()) {
                minHeap.remove(existing.get());
                minHeap.add(new Node(key, maxCount));
                isHot = true;
            } else {
                if (minHeap.size() < k || maxCount >= Objects.requireNonNull(minHeap.peek()).count) {
                    Node newNode = new Node(key, maxCount);
                    if (minHeap.size() >= k) {
                        expelled = minHeap.poll().key;
                        expelledQueue.offer(new Item(expelled, maxCount));
                    }
                    minHeap.add(newNode);
                    isHot = true;
                }
            }

            return new AddResult(expelled, isHot, key);
        }
    }

    @Override
    public List<Item> list() {
        synchronized (minHeap){
            List<Item> result = new ArrayList<>(minHeap.size());
            for(Node node : minHeap){
                result.add(new Item(node.key,node.count));
            }
            result.sort(Comparator.comparingInt(Item::count).reversed());
            return result;
        }
    }

    @Override
    public BlockingQueue<Item> expelled() {
        return expelledQueue;
    }

    /**
     * 对所有桶的计数进行减半操作，
     */
    @Override
    public void fading() {
        for(Bucket[] row : buckets){
            for(Bucket bucket : row){
                synchronized (bucket){
                    bucket.count = bucket.count >> 1;
                }
            }
        }

        synchronized (minHeap){
            PriorityQueue<Node> newHeap = new PriorityQueue<>(Comparator.comparingInt(n -> n.count));
            for(Node node : minHeap){
                newHeap.add(new Node(node.key,node.count >> 1));
            }
            minHeap.clear();
            minHeap.addAll(newHeap);
        }

        total = total >> 1;
    }

    @Override
    public long total() {
        return total;
    }

    private static class Bucket {
        /**
         * 用于存储键的指纹，
         * 指纹是一个64位的整数，
         * 用于快速比较两个键是否相同。
         */
        long fingerprint;
        /**
         * 用于存储键的计数，
         * 计数是一个32位的整数，
         * 用于表示键的访问次数。
         */
        int count;
    }

    private static class Node {
        final String key;
        final int count;

        Node(String key, int count) {
            this.key = key;
            this.count = count;
        }
    }

    private static int hash(byte[] data) {
        return HashUtil.murmur32(data);
    }

    public static void main(String[] args) {
        String input = "test";
        byte[] data = input.getBytes();

        int hash1 = HashUtil.murmur32(data);
        int hash2 = HashUtil.murmur32(data);

        System.out.println("第一次哈希值: " + hash1);
        System.out.println("第二次哈希值: " + hash2);
        System.out.println("两次哈希值是否相同: " + (hash1 == hash2));
    }
}

// 新增返回结果类
@Data
class AddResult {
    // 被挤出的 key
    private final String expelledKey;
    // 当前 key 是否进入 TopK
    private final boolean isHotKey;
    // 当前操作的 key
    private final String currentKey;

    public AddResult(String expelledKey, boolean isHotKey, String currentKey) {
        this.expelledKey = expelledKey;
        this.isHotKey = isHotKey;
        this.currentKey = currentKey;
    }

}
