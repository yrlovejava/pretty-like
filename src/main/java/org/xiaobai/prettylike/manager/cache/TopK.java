package org.xiaobai.prettylike.manager.cache;

import java.util.List;
import java.util.concurrent.BlockingQueue;

/**
 * TopK 接口
 */
public interface TopK {
    AddResult add(String key, int increment);
    List<Item> list();
    BlockingQueue<Item> expelled();
    void fading();
    long total();
}
