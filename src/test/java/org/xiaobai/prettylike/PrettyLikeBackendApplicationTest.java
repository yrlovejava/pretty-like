package org.xiaobai.prettylike;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.xiaobai.prettylike.model.entity.Blog;
import org.xiaobai.prettylike.model.entity.Thumb;
import org.xiaobai.prettylike.model.entity.User;
import org.xiaobai.prettylike.service.BlogService;
import org.xiaobai.prettylike.service.ThumbService;
import org.xiaobai.prettylike.service.UserService;
import org.xiaobai.prettylike.utils.SnowflakeIdGenerator;

import java.util.*;

@Slf4j
@SpringBootTest
public class PrettyLikeBackendApplicationTest {

    @Resource
    private ThumbService thumbService;

    @Resource
    private UserService userService;

    @Resource
    private BlogService blogService;

    @Test
    void contextLoads() {
        System.out.println(thumbService.list());
        System.out.println(userService.list());
        System.out.println(blogService.list());
    }

    @Test
    void initData() {
        // 初始化雪花算法ID生成器
        SnowflakeIdGenerator idGenerator = new SnowflakeIdGenerator(1, 1);
        // 用户
        List<User> userList = new ArrayList<>();
        for(int i = 0;i < 100000;i++){
            User user = new User();
            user.setId(idGenerator.nextId());
            user.setUsername(generateRandomUsername());
            userList.add(user);
        }
        userService.saveBatch(userList);
        log.info("用户初始化完成，一共生成 {} 条用户数据", userList.size());

        // 博客
        List<Blog> blogList = new ArrayList<>();
        Random random = new Random();
        for(int i = 0;i < 10000;i++){
            Blog blog = new Blog();
            blog.setId(idGenerator.nextId());
            blog.setUserId(userList.get(random.nextInt(userList.size())).getId());
            blog.setContent(generateRandomContent());
            blog.setTitle(generateRandomTitle());
            String coverImg = "https://picsum.photos/seed/blog" + i + "/800/400";
            blog.setCoverImg(coverImg);
            blog.setCreateTime(generateRandomDate());
            blog.setThumbCount(0);
            blogList.add(blog);
        }
        blogService.saveBatch(blogList);
        log.info("博客初始化完成,一共生成 {} 条博客数据", blogList.size());

        // 点赞
        List<Thumb> thumbList = new ArrayList<>();
        int thumbCount = 50000; // 生成50000条点赞数据
        Set<String> uniqueThumbs = new HashSet<>(); // 用于保证userId-blogId组合唯一
        while(thumbList.size() < thumbCount) {
            long userId = userList.get(random.nextInt(userList.size())).getId();
            long blogId = blogList.get(random.nextInt(blogList.size())).getId();
            String key = userId + "-" + blogId;

            // 确保每个用户对同一博客只点赞一次
            if(!uniqueThumbs.contains(key)) {
                uniqueThumbs.add(key);

                Thumb thumb = new Thumb();
                thumb.setUserId(userId);
                thumb.setBlogId(blogId);
                thumb.setCreateTime(generateRandomDate());
                thumbList.add(thumb);

                // 更新博客点赞数（可选，实际业务中可能通过数据库函数或查询统计）
                for(Blog blog : blogList) {
                    if(blog.getId().equals(blogId)) {
                        blog.setThumbCount(blog.getThumbCount() + 1);
                        break;
                    }
                }
            }
        }
        thumbService.saveBatch(thumbList);
        blogService.updateBatchById(blogList); // 更新博客点赞数
        log.info("点赞初始化完成,一共生成 {} 条点赞数据", thumbList.size());
    }

    private static String generateRandomUsername() {
        String[] prefixes = {"user", "blogger", "writer", "author", "reader"};
        Random random = new Random();
        return prefixes[random.nextInt(prefixes.length)] + random.nextInt(10000);
    }

    private static String generateRandomTitle() {
        String[] topics = {"技术", "生活", "旅行", "美食", "教育", "健康", "科学", "艺术"};
        String[] actions = {"探索", "分享", "学习", "体验", "思考", "发现", "讨论"};
        String[] endings = {"的乐趣", "的方法", "的技巧", "的经验", "的故事", "的秘密", "的未来"};

        Random random = new Random();
        return topics[random.nextInt(topics.length)] +
                actions[random.nextInt(actions.length)] +
                endings[random.nextInt(endings.length)];
    }

    private static String generateRandomContent() {
        StringBuilder content = new StringBuilder();
        Random random = new Random();
        int paragraphCount = random.nextInt(5) + 1;

        for (int i = 0; i < paragraphCount; i++) {
            int sentenceCount = random.nextInt(5) + 3;
            for (int j = 0; j < sentenceCount; j++) {
                content.append(generateRandomSentence()).append(" ");
            }
            content.append("\n\n");
        }
        return content.toString();
    }

    private static String generateRandomSentence() {
        String[] subjects = {"我们", "你们", "他们", "我", "你", "这个博客", "这篇文章"};
        String[] verbs = {"讨论", "分享", "介绍", "分析", "讲述", "探索", "研究"};
        String[] objects = {"技术趋势", "生活方式", "旅行经历", "美食文化", "教育方法", "健康知识", "科学发现"};
        String[] endings = {"。", "！", "？", "，让我们一起思考。", "，欢迎分享你的看法。"};

        Random random = new Random();
        return subjects[random.nextInt(subjects.length)] +
                verbs[random.nextInt(verbs.length)] +
                objects[random.nextInt(objects.length)] +
                endings[random.nextInt(endings.length)];
    }

    private static Date generateRandomDate() {
        Random random = new Random();
        long now = System.currentTimeMillis();
        // 生成过去1年内的随机时间
        long oneYearAgo = now - 365L * 24 * 60 * 60 * 1000;
        long randomTime = oneYearAgo + random.nextLong(now - oneYearAgo);
        return new Date(randomTime);
    }
}
