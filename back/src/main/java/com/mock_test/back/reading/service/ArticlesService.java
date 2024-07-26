package com.mock_test.back.reading.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mock_test.back.reading.dto.AddReadingDTO;
import com.mock_test.back.reading.dto.QuestionDTO;
import com.mock_test.back.reading.dto.ReadingDTO;
import com.mock_test.back.reading.model.Article;
import com.mock_test.back.reading.model.Question;
import com.mock_test.back.reading.model.ReadingTest;
import com.mock_test.back.reading.model.Selection;
import com.mock_test.back.reading.repository.ArticlesRepository;
import com.mock_test.back.reading.repository.QuestionRepository;
import com.mock_test.back.reading.repository.SelectionRepository;
import com.mock_test.back.redis.service.RedisHashService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ArticlesService {

    @Autowired
    ArticlesRepository articlesRepository;

    @Autowired
    QuestionRepository questionRepository;

    @Autowired
    SelectionRepository selectionRepository;

    @Autowired
    RedisHashService redisHashService;

    public ReadingTest startReadingTest() {
        ReadingTest result = redisHashService.get();
        if (ObjectUtils.isEmpty(result) || ObjectUtils.isEmpty(result.getId())) {
            List<Article> articles = articlesRepository.findByIsDoneFalse();
            AtomicInteger index = new AtomicInteger(0);
            List<QuestionDTO> list = articles.stream().flatMap(item -> {
                List<Question> questions = questionRepository.findByArticleIdOrderBySequenceAsc(item.getId());
                return questions.stream().map(question -> {
                    int currentIndex = index.incrementAndGet();
                    return QuestionDTO.builder()
                            .articleId(item.getId())
                            .questionId(question.getId())
                            .question(question.getQuestion())
                            .index(currentIndex)
                            .build();
                });
            }).toList();
            return redisHashService.createReadingTest(list);
        }
        this.resetRemainTime(result);
        redisHashService.saveOrUpdate(result);
        return result;
    }

    public ReadingTest next(Integer index, Integer option) {
        ReadingTest result = redisHashService.get();
        if (ObjectUtils.isEmpty(result) || ObjectUtils.isEmpty(result.getId())) {
            return null;
        }
        int nextIndex = index + 1;
        result.setIndex(nextIndex);
        result.setCurrentArticleId(result.getQuestions()
                .stream()
                .filter(item -> item.getIndex() == nextIndex)
                .findFirst()
                .orElseThrow()
                .getArticleId());
        result.getQuestions().stream()
                .filter(item -> Objects.equals(item.getIndex(), index))
                .findFirst()
                .ifPresent(question -> {
                    if (option != 0) {
                        question.setMySelection(option);
                        question.setSelected(true);
                    }
                });
        this.resetRemainTime(result);
        redisHashService.saveOrUpdate(result);
        return result;
    }

    public ReadingTest back(Integer index) {
        ReadingTest result = redisHashService.get();
        if (ObjectUtils.isEmpty(result) || ObjectUtils.isEmpty(result.getId())) {
            return null;
        }
        int preIndex = index - 1;
        result.setIndex(preIndex);
        result.setCurrentArticleId(result.getQuestions()
                .stream()
                .filter(item -> item.getIndex() == preIndex)
                .findFirst()
                .orElseThrow()
                .getArticleId());
        this.resetRemainTime(result);
        redisHashService.saveOrUpdate(result);
        return result;
    }


    public ReadingDTO getQuestion(Integer id, Integer questionNum) {
        List<ReadingTest.QuestionDetail> questions = redisHashService.get().getQuestions();

        int index = questionNum - 10 > 0 ? questionNum - 11 : questionNum - 1;
        Article article = articlesRepository.getReferenceById(id);
        Question question = questionRepository.findByArticleIdOrderBySequenceAsc(article.getId()).get(index);
        List<Selection> selections = selectionRepository.findByQuestionId(question.getId());
        List<ReadingDTO.SelectionDTO> selectionDTOS = new ArrayList<>();
        selections.forEach(item -> selectionDTOS.add(ReadingDTO.SelectionDTO.builder()
                .id(item.getId())
                .information(item.getInformation())
                .build()));

        return ReadingDTO.builder()
                .id(article.getId())
                .heading(article.getHeading())
                .context(article.getContext())
                .questionId(question.getId())
                .paragraphNum(question.getParagraphNum())
                .question(question.getQuestion())
                .type(question.getType())
                .selections(selectionDTOS)
                .build();
    }

    @Transactional
    public void add(AddReadingDTO dto) {
        Article article = articlesRepository.save(Article.builder()
                .heading(dto.getHeading())
                .context(dto.getContext())
                .isDone(false)
                .build());

        List<Selection> selections = new ArrayList<>();

        AtomicInteger paragraphNum = new AtomicInteger(1);
        dto.getQuestions().forEach(questionDTO -> {
            int currentParagraphNum = this.getParagraphNum(paragraphNum.get(), questionDTO);
            paragraphNum.set(currentParagraphNum);
            Question question = questionRepository.save(Question.builder()
                    .articleId(article.getId())
                    .paragraphNum(this.getParagraphNum(currentParagraphNum, questionDTO))
                    .question(StringUtils.capitalize(questionDTO.getQuestion()))
                    .sequence(questionDTO.getSequence())
                    .type(this.checkQuestionType(questionDTO))
                    .correctAnswer(questionDTO.getCorrectAnswer())
                    .build());

            questionDTO.getSelections().forEach(item -> {
                selections.add(Selection.builder()
                        .information(StringUtils.capitalize(item.getInformation()))
                        .questionId(question.getId())
                        .questionSequence(question.getSequence())
                        .option(item.getOption())
                        .isCorrect(questionDTO.getCorrectAnswer().contains(item.getOption().toString()))
                        .build());
            });
        });
        selectionRepository.saveAll(selections);
    }

    private int getParagraphNum(int paragraphNum, AddReadingDTO.QuestionDTO questionDTO) {
        if (questionDTO.getQuestion().contains("paragraph")) {
            paragraphNum = Integer.parseInt(Objects.requireNonNull(this.getParagraphNumber(questionDTO.getQuestion())));
        }
        if (questionDTO.getSequence() == 10) {
            paragraphNum = 0;
        }
        return paragraphNum;
    }

    private Question.Type checkQuestionType(AddReadingDTO.QuestionDTO questionDTO) {
        Question.Type type = Question.Type.SELECTION;
        if (questionDTO.getQuestion().contains("The word")) {
            type = Question.Type.VOCABULARY;
        }
        if (questionDTO.getQuestion().contains("TWO")) {
            type = Question.Type.MULTIPLE_CHOICE;
        }
        if (questionDTO.getSequence() == 9) {
            type = Question.Type.INSERTION;
        }
        if (questionDTO.getSequence() == 10) {
            type = Question.Type.DRAG;
        }
        return type;
    }

    private String getParagraphNumber(String input) {
        Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(input);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    private void resetRemainTime(ReadingTest result) {
        long remainTime = (36 * 60) - Duration.between(LocalDateTime.parse(result.getStartTime()),
                LocalDateTime.now()).getSeconds();
        result.setRemainTime(remainTime);
    }
}
