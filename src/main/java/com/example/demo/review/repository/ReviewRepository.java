package com.example.demo.review.repository;

import com.example.demo.lecture.Lecture;
import com.example.demo.review.Review;
import com.example.demo.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long>, CustomReviewRepository {
    Optional<Review> findByUserAndLecture(User user, Lecture lecture);
    List<Review> findByLecture(Lecture lecture);
}