package com.example.demo.review.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewPostDto {
    private String comment;
    private String commentTitle;
    private int rate;
}
