package com.example.demo.mypage.dto;

import com.example.demo.lecture.Lecture;
import com.example.demo.lecture.dto.AllLecturesResponse;
import com.example.demo.user.User;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class MyPageResponse {
    private String userNickname;
    private String userProfileImg;
    private String githubUrlName;
    private String userCompany;
    private List<LikedLecturesResponse> likedLectures;
    private List<LikedStudiesResponse> likedStudies;
    private List<LikedRoadmapsResponse> likedRoadmaps;
    private List<MyReviewsResponse> myReviews;
    private List<MyStudiesResponse> myStudies;
    private List<MyRoadmapsResponse> myRoadmaps;

//    public static MyPageResponse from(Lecture lecture, User user){
//        return MyPageResponse.builder()
//                .userNickname(builder().userNickname)
//                .userProfileImg(user.getUserProfileImg())
//                .githubUrlName(user.getGithubUrlName())
//                .userCompany(user.getUserCompany())
//                .likedLectures()
//                .build();
//    }
}
