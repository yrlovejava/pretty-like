package org.xiaobai.prettylike;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.annotation.Resource;
import jakarta.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.xiaobai.prettylike.model.entity.Blog;
import org.xiaobai.prettylike.model.entity.Thumb;
import org.xiaobai.prettylike.model.entity.User;
import org.xiaobai.prettylike.service.BlogService;
import org.xiaobai.prettylike.service.ThumbService;
import org.xiaobai.prettylike.service.UserService;
import org.xiaobai.prettylike.utils.SnowflakeIdGenerator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.*;


import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
public class PrettyLikeBackendApplicationTest {

    @Resource
    private ThumbService thumbService;

    @Resource
    private UserService userService;

    @Resource
    private BlogService blogService;

    @Resource
    private MockMvc mockMvc;

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

    @Test
    void testLoginAndExportSessionToCsv() throws Exception {
        List<User> list = userService.list();

        try (PrintWriter writer = new PrintWriter(new FileWriter("session_output.csv", true))) {
            // 如果文件是第一次写入，你也可以加一个逻辑写表头
            writer.println("userId,sessionId,timestamp");

            for (User user : list) {
                long testUserId = user.getId();

                MvcResult result = mockMvc.perform(get("/user/login")
                                .param("userId", String.valueOf(testUserId))
                                .contentType(MediaType.APPLICATION_JSON))
                        .andReturn();

                List<String> setCookieHeaders = result.getResponse().getHeaders("Set-Cookie");
                assertThat(setCookieHeaders).isNotEmpty();

                String sessionId = setCookieHeaders.stream()
                        .filter(cookie -> cookie.startsWith("SESSION")) // Spring Session 默认是 SESSION（不是 JSESSIONID）
                        .map(cookie -> cookie.split(";")[0]) // SESSION=xxx
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("No SESSION found in response"));

                String sessionValue = sessionId.split("=")[1];

                writer.printf("%d,%s,%s%n", testUserId, sessionValue, LocalDateTime.now());

                System.out.println("✅ 写入 CSV：" + testUserId + " -> " + sessionValue);
            }
        }
    }

    /**
     * 生成压测使用的点赞数据
     */
    @Test
    void initDoLikeData() throws IOException {
        List<Long> allBlogIds = blogService.list(Wrappers.<Blog>lambdaQuery().select(Blog::getId))
                .stream()
                .map(Blog::getId)
                .toList();
        int recordsPerBlogId = 1000;
        String outputPath = "jemeter/like.csv";

        File dir = new File(outputPath).getParentFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(outputPath, false))) {
            Random random = new Random();

            // 将blogId分为热点组和普通组（80/20规则）
            int hotCount = Math.max(1, allBlogIds.size() / 5); // 20%作为热点
            List<Long> hotBlogIds = allBlogIds.subList(0, hotCount);
            List<Long> normalBlogIds = allBlogIds.subList(hotCount, allBlogIds.size());

            // 生成数据：80%请求分配给热点组，20%给普通组
            int totalRecords = allBlogIds.size() * recordsPerBlogId;
            int hotRecords = (int) (totalRecords * 0.8);
            int normalRecords = totalRecords - hotRecords;

            // 生成热点数据
            for (int i = 0; i < hotRecords; i++) {
                int randomIndex = random.nextInt(hotBlogIds.size());
                writer.println(hotBlogIds.get(randomIndex));
            }

            // 生成普通数据
            for (int i = 0; i < normalRecords; i++) {
                int randomIndex = random.nextInt(normalBlogIds.size());
                writer.println(normalBlogIds.get(randomIndex));
            }

            System.out.println("✅ 生成完成：" + totalRecords + " 条带热点的点赞数据");
        }
    }
}
