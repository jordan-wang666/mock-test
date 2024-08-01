package com.mock_test.back.redis.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mock_test.back.listening.model.ListeningTest;
import com.mock_test.back.reading.dto.QuestionDTO;
import com.mock_test.back.reading.model.ReadingTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class RedisHashService {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private final static String READING = "READING";
    private final static String LISTENING = "LISTENING";

    public ReadingTest createReadingTest(List<QuestionDTO> list) {
        String id = UUID.randomUUID().toString();
        ReadingTest readingTest = new ReadingTest();
        readingTest.setId(id);
        readingTest.setStartTime(LocalDateTime.now().toString());
        readingTest.setRemainTime(36 * 60);
        readingTest.setIndex(1);
        readingTest.setTotal(20);
        readingTest.setCurrentArticleId(list.get(0).getArticleId());
        readingTest.setSequence(list.get(0).getSequence());

        // Convert QuestionDTO list to List<QuestionDetail>
        List<ReadingTest.QuestionDetail> questions = list.stream().map(item -> {
            ReadingTest.QuestionDetail questionDetail = new ReadingTest.QuestionDetail();
            questionDetail.setIndex(item.getIndex());
            questionDetail.setId(item.getQuestionId());
            questionDetail.setQuestion(item.getQuestion());
            questionDetail.setArticleId(item.getArticleId());
            return questionDetail;
        }).toList();

        readingTest.setQuestions(questions);

        this.saveOrUpdateReading(readingTest);
        redisTemplate.expire(READING, 2, TimeUnit.HOURS);
        return readingTest;
    }

    public void createListeningTest(String id) {
        ListeningTest test = new ListeningTest();
        test.setId(id);
        this.saveOrUpdateListening(test);
        redisTemplate.expire(LISTENING, 2, TimeUnit.HOURS);
    }

    public void startListeningTest(ListeningTest dto) {
        Map<String, Object> hash = objectMapper.convertValue(dto, new TypeReference<>() {
        });
        redisTemplate.opsForHash().putAll(LISTENING, hash);
    }

    public ReadingTest getReading() {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(READING);
        Map<String, Object> result = new HashMap<>();
        entries.forEach((key, value) -> result.put(key.toString(), value));
        return objectMapper.convertValue(result, ReadingTest.class);
    }

    public ListeningTest getListening() {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(LISTENING);
        Map<String, Object> result = new HashMap<>();
        entries.forEach((key, value) -> result.put(key.toString(), value));
        return objectMapper.convertValue(result, ListeningTest.class);
    }

    public void delReading() {
        redisTemplate.delete(READING);
    }

    public void saveOrUpdateReading(ReadingTest readingTest) {
        Map<String, Object> hash = objectMapper.convertValue(readingTest, new TypeReference<>() {
        });
        redisTemplate.opsForHash().putAll(READING, hash);
    }

    public void saveOrUpdateListening(ListeningTest readingTest) {
        Map<String, Object> hash = objectMapper.convertValue(readingTest, new TypeReference<>() {
        });
        redisTemplate.opsForHash().putAll(LISTENING, hash);
    }
}
