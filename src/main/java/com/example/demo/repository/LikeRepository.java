package com.example.demo.repository;

import com.example.demo.domain.Like;
import com.example.demo.domain.StudyPost;
import com.example.demo.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {
    List<Like> findAllLikeByUser(User user); // user기반으로 모든 관심글 찾아오기
    List<Like> findAllLikeByStudyPost(StudyPost post); //한 스터디글에 대한 모든 좋아요 정보 가져오기
}