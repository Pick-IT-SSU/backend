package com.example.demo.lecture;

import com.example.demo.hashtag.repository.HashtagRepository;
import com.example.demo.lecture.dto.AllLecturesResponse;
import com.example.demo.lecture.dto.DetailLectureResponse;
import com.example.demo.lecture.dto.ExcelData;
import com.example.demo.lecture.dto.RecLecturesResponse;
import com.example.demo.like.Like;
import com.example.demo.like.repository.LikeRepository;
import com.example.demo.reviewHashtag.ReviewHashtag;
import com.example.demo.review.dto.DetailReviewResponse;
import com.example.demo.hashtag.Hashtag;
import com.example.demo.lecture.repository.LectureRepository;
import com.example.demo.review.Review;
import com.example.demo.review.repository.ReviewRepository;
import com.example.demo.reviewHashtag.ReviewHashtagRepository;
import com.example.demo.user.User;
import lombok.RequiredArgsConstructor;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class LectureService {
    private final LectureRepository lectureRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewHashtagRepository reviewHashtagRepository;
    private final HashtagRepository hashtagRepository;
    private final LikeRepository likeRepository;

    // 추천용 강의 데이터 가공 함수
    public List<RecLecturesResponse> manageRecommendData(){
        List<RecLecturesResponse> recLectures = new ArrayList<>();
        List<AllLecturesResponse> lectures = this.getLectures(); // 전체글에서 필터링해보기
        for(int i=0;i<lectures.size();i++){
            Long lectureId = lectures.get(i).getLectureId();
            Lecture lecture = findById(lectureId);
            RecLecturesResponse recLecturesResponse = new RecLecturesResponse();
            BeanUtils.copyProperties(lectures.get(i), recLecturesResponse,"thumbnailUrl", "likeCnt"); // 원본 객체, 복사 대상 객체
            recLecturesResponse.setHashtags(getBestHashtags(lecture)); // 특정 Lecture에 해당하는 해시태그 상위 3개 가져오는 함수 호출
            recLecturesResponse.setReviewCnt(getReviewCnt(lecture));
            recLectures.add(recLecturesResponse);
        }
        return recLectures;
    }

    // 전체 강의 조회
    public List<AllLecturesResponse> getLectures(){
        List<AllLecturesResponse> allLectures = new ArrayList<>();
        List<Lecture> lectures = lectureRepository.findAll();
        for(int i=0;i<lectures.size();i++){
            AllLecturesResponse lecture = new AllLecturesResponse();
            lecture.setLectureId(lectures.get(i).getLectureId());
            lecture.setLectureTitle(lectures.get(i).getLectureTitle());
            lecture.setThumbnailUrl(lectures.get(i).getThumbnailUrl());
            lecture.setLikeCnt(lectures.get(i).getLikes().size());
            lecture.setAvgRate(getAvgRate(lectures.get(i))); // 리뷰평점 평균
            allLectures.add(lecture);
        }
        return allLectures;
    }

    // 검색어별 조회
    public List<AllLecturesResponse> getFilteredLectures(String keyword, String category){
        List<AllLecturesResponse> allLectures = new ArrayList<>();
        List<AllLecturesResponse> lectures = this.getLectures(); // 전체글에서 필터링해보기

        if(keyword!=null){ // 키워드만 있는 경우
            String[] keywords = keyword.split(" ");
            for(int i=0;i<keywords.length;i++){
                String word = keywords[i];
                allLectures.addAll(lectures.stream() // 제목에 키워드 포함된 거 가져오기
                        .filter(lecture -> lecture.getLectureTitle().contains(word))
                        .collect(Collectors.toList()));
            }
        }

        if(category!=null){ // 카테고리(해시태그)만 있는 경우
            List<String> categories = Arrays.asList(category.split(",")); // 카테고리 받아온거
            for(int i=0;i<lectures.size();i++) { // 강의 전체를 돌면서
                Lecture lecture = this.findById(lectures.get(i).getLectureId());
                List<String> hashtags = this.getBestHashtags(lecture); // 강의의 해시태그 3개 가져오기
                List<String> finalList = hashtags.stream()
                        .filter(element -> listContains(categories, element)) // 사용자가 원하는 카테고리에 해당 강의의 hashtag 중 하나라도 포함되어 있는 경우
                        .collect(Collectors.toList());
                if(finalList.isEmpty()) { // 포함되는게 없는 것은 빼기
                    lectures.remove(i--); // remove 할 때 인덱스도 같이 줄여줌
                }
            }
            allLectures.addAll(lectures);
        }
        return allLectures;
    }

    public static <T> boolean listContains(List<T> array, T element) { // categories / hashtag 중 하나
        // (1,2,3) in (3,4,5) -> 3 출력
        return array.stream()
                .filter(e -> e.equals(element)) // categories 의 category에서 hashtag랑 같은거
                .findFirst().isPresent();
    }

    // 특정 강의 조회
    public Lecture findById(Long lectureId){
        Optional<Lecture> lecture = lectureRepository.findById(lectureId);
        return lecture.orElse(null);
    }

    // 강의글 상세 조회
    public DetailLectureResponse getLecture(long lectureId, long userId){
        DetailLectureResponse detailLectureResponse = new DetailLectureResponse();
        Optional<Lecture> optionalLecture = lectureRepository.findById(lectureId); // lecture 데이터 가져와서
        if(optionalLecture.isEmpty())
            return detailLectureResponse;
        Lecture lecture = optionalLecture.get();
        detailLectureResponse.setLectureId(lecture.getLectureId());
        detailLectureResponse.setLectureTitle(lecture.getLectureTitle());
        detailLectureResponse.setLecturer(lecture.getLecturer());
        detailLectureResponse.setSiteName(lecture.getSiteName());
        detailLectureResponse.setLectureUrl(lecture.getLectureUrl());
        detailLectureResponse.setThumbnailUrl(lecture.getThumbnailUrl());
        detailLectureResponse.setReviewCnt(getReviewCnt(lecture)); // 리뷰 개수 세팅

        List<Review> reviews = reviewRepository.findByLecture(lecture); // lecture 를 갖고 reviews 에 있는 모든 데이터 가져오기
        double totalRate=0;
        List<DetailReviewResponse> detailReviewResponses = new ArrayList<>();
        for(int i=0;i<reviews.size();i++){ // 특정 강의에 해당하는 리뷰들을 찾기 위해서
            DetailReviewResponse detailReviewResponse = new DetailReviewResponse(); // 해당 리뷰글 내가 쓴건지 니가 쓴건지 구분해야함
            BeanUtils.copyProperties(reviews.get(i), detailReviewResponse,"reviewHashtags"); // 원본 객체, 복사 대상 객체
            String nickname = reviews.get(i).getUser().getUserNickname();
            detailReviewResponse.setNickname(nickname);

            if(userId == reviews.get(i).getUser().getUserId()) // 리뷰 등록자와 로그인한 사용자가 같다면
                detailReviewResponse.setWriterStatus(true);
            else
                detailReviewResponse.setWriterStatus(false);
            detailReviewResponses.add(detailReviewResponse);
            totalRate += reviews.get(i).getRate();
        }
        detailLectureResponse.setReviews(detailReviewResponses);
        detailLectureResponse.setHashtags(getBestHashtags(lecture)); // 특정 Lecture에 해당하는 해시태그 상위 3개 가져오는 함수 호출
        detailLectureResponse.setAvgRate(totalRate/reviews.size()); // 평균 점수 계산

        List<Like> likes = likeRepository.findLikeByLecture(lecture);
        detailLectureResponse.setLikeCnt(likes.size());
        return detailLectureResponse;
    }

    // 강의 등록
    public long saveLecture(Lecture lecture){
        Lecture savedLecture = lectureRepository.save(lecture);
        return savedLecture.getLectureId();
    }

    // 해시태그 저장
    public void manageHashtag(List<String> hashtags, Review review){
        for (int i = 0; i < hashtags.size(); i++) {
            Optional<Hashtag> existedHashtag = hashtagRepository.findByHashtagName((hashtags.get(i)));
            ReviewHashtag reviewHashtag = new ReviewHashtag();
            if(existedHashtag.isPresent()) { // 이미 들어간 해시태그라면 id 받아오기
                reviewHashtag.setHashtag(existedHashtag.get());
            }
            else { // 없는 해시태그라면 해시태그를 생성하고 나서 reviewHashtag 에 넣기
                Hashtag hashtag = new Hashtag(hashtags.get(i));
                hashtagRepository.save(hashtag);
                reviewHashtag.setHashtag(hashtag);
            }
            reviewHashtag.setReview(review);
            reviewHashtagRepository.save(reviewHashtag);
        }
    }

//    public List<String> getHashtags()

    // 특정 Lecture에 해당하는 해시태그 상위 3개 가져오는 함수
    public List<String> getBestHashtags(Lecture lecture){
        List<Review> reviews = reviewRepository.findByLecture(lecture); // lecture 를 갖고 reviews 에 있는 모든 데이터 가져오기
        Map<Long, Integer> hashtagCnt = new HashMap<>(); // 해시태그 상위 3개 찾기 위해서
        for(int i=0;i<reviews.size();i++){ // 특정 강의에 해당하는 리뷰들을 돌면서 해시태그 개수 세기
            List<ReviewHashtag> reviewHashtags = reviewHashtagRepository.findByReview(reviews.get(i));

            for(int j=0;j<reviewHashtags.size();j++){
                long hashtagId = reviewHashtags.get(j).getHashtag().getHashtagId();
                if(hashtagCnt.containsKey(hashtagId)){                 // 이미 키 값이 존재하면 해당 value + 1
                    int cnt = hashtagCnt.get(hashtagId);
                    hashtagCnt.put(hashtagId, cnt+1);
                }
                else{ // 키가 존재하지 않는 경우
                    hashtagCnt.put(hashtagId, 1);
                }
            }
        }

        // hashmap 내림차순 정렬 후 3개까지만 자르기
        List<Map.Entry<Long, Integer>> entryList = new LinkedList<>(hashtagCnt.entrySet());
        entryList.sort((o1, o2) -> hashtagCnt.get(o1.getKey()) - hashtagCnt.get(o2.getKey()));
        int limit = 0;
        List<String> hashtags = new ArrayList<>(); // hashtag 담을 list 생성
        for(Map.Entry<Long, Integer> entry : entryList){
            if(limit == 3)
                break;
            Optional<Hashtag> hashtag = hashtagRepository.findById(entry.getKey());
            String hashtagName = hashtag.get().getHashtagName();
            hashtags.add(hashtagName);
            limit++;
        }
        return hashtags;
    }

    // 평균 평점 계산
    public double getAvgRate(Lecture lecture){
        List<Review> reviews = reviewRepository.findByLecture(lecture); // lecture 를 갖고 reviews 에 있는 모든 데이터 가져오기
        double totalRate = 0;
        for(int i=0;i<reviews.size();i++){ // 특정 강의에 해당하는 리뷰들을 찾기 위해서
            totalRate += reviews.get(i).getRate();
        }
        return totalRate/reviews.size(); // 평균 점수 계산
    }

    // 리뷰 개수 세기
    public int getReviewCnt(Lecture lecture){
        List<Review> reviews = reviewRepository.findByLecture(lecture); // lecture 를 갖고 reviews 에 있는 모든 데이터 가져오기
        return reviews.size();
    }

    // url 중복 조회용
    public Lecture findByUrl(String lectureUrl){
        Optional<Lecture> lecture = lectureRepository.findBylectureUrl(lectureUrl);
        return lecture.orElse(null);
    }
}
