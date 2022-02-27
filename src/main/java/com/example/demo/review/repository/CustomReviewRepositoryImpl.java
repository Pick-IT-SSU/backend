package com.example.demo.review.repository;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import static com.example.demo.review.QReview.review;

@Repository
@RequiredArgsConstructor
public class CustomReviewRepositoryImpl implements CustomReviewRepository{
    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public void updateReview(String commentTitle, String comment, Long reviewId){
        jpaQueryFactory
                .update(review)
                .set(review.commentTitle, commentTitle)
                .set(review.comment, comment)
                .where(review.reviewId.eq(reviewId))
                .execute();
    }

    @Override
    public void deleteReview(Long reviewId){
        jpaQueryFactory
                .update(review)
                .set(review.reviewStatus, 0)
                .where(review.reviewId.eq(reviewId))
                .execute();
    }
}