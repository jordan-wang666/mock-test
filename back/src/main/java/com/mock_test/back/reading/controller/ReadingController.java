package com.mock_test.back.reading.controller;

import com.mock_test.back.reading.dto.ReadingDTO;
import com.mock_test.back.reading.service.ArticlesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reading")
public class ReadingController {

    @Autowired
    ArticlesService articlesService;

    @GetMapping("/{id}/{questionNum}")
    ReadingDTO getById(@PathVariable("id") Integer id, @PathVariable("questionNum") Integer questionNum) {
        return articlesService.getQuestion(id, questionNum);
    }
}