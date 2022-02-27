package com.example.demo.study.service;

import com.example.demo.study.domain.StudyPost;
import com.example.demo.study.dto.AllStudyPostsResponse;
import com.example.demo.study.dto.StudyPostDTO;
import com.example.demo.user.dto.UserOnlyDto;
import com.example.demo.study.repository.StudyPostRepository;
import lombok.AllArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
@Transactional
public class StudyPostService {
    private final StudyPostRepository studyPostRepository;

    public long saveStudyPost(StudyPost post){
        StudyPost studyPost = studyPostRepository.save(post);
        return studyPost.getStudyPostId();
    }

    public StudyPost findStudyPostById(Long postId){
        Optional<StudyPost> post = studyPostRepository.findById(postId); // status 0이면 null return하게 바꾸기
        if(post.isPresent()){
            StudyPost studyPost=post.get();
            if(studyPost.getStudyStatus()==0){
                return null;
            }else{
                return studyPost;
            }
        }else{
            return null;
        }
    }

    public StudyPost modifyStudyPost(StudyPostDTO postDTO,Long postId){
        StudyPost post=findStudyPostById(postId);
        if(post!=null){ //post의 status가 1인 경우에만 set
            post.setStudyPost(postDTO);
            saveStudyPost(post);
            post=findStudyPostById(postId);
            return post;
        }else{
            return null;
        }
    }

    public List<StudyPost> getAllStudyPosts(){
        List<StudyPost> studyPosts = studyPostRepository.findAll();
        if(studyPosts.isEmpty()){
            return studyPosts;
        }

        //remove를 하는 경우에 enchanced for loop을 사용하면? remove의 fastRemove 에서 데이터 조작으로 인한 오류 발생 -> iterator를 사용하자!
        Iterator<StudyPost> itr=studyPosts.iterator();
        while(itr.hasNext()){
            StudyPost post=itr.next();
            if(post.getStudyStatus()==0){
                itr.remove(); // iterator를 사용하는 경우, StudyPosts자체에 대해 remove를 사용하면 오류가 발생한다. -> 반드시 iterator 자체에 대해서 remove를 수행해야함
            }
        }
        return studyPosts;
    }

    public List<StudyPost> getStudyPostsWithFilter(String originCategories, String originKeywords, String location){
        String[] categories=null;
        String[] keywords=null;
        if(originCategories!=null){
            categories=originCategories.split(",");
        }
        if(originKeywords!=null){
            keywords=originKeywords.split(" ");
        }
        return studyPostRepository.findPostsByTest(categories,keywords,location);
    }

    public List<AllStudyPostsResponse> getAllStudiesResponse(List<StudyPost> studyPostList){
        List<AllStudyPostsResponse> studiesResponseList=new ArrayList<>();
        for(StudyPost post:studyPostList){
            AllStudyPostsResponse studyResponse=new AllStudyPostsResponse();
            UserOnlyDto userDto=new UserOnlyDto();
            BeanUtils.copyProperties(post,studyResponse);
            BeanUtils.copyProperties(post.getUser(),userDto);
            studyResponse.setUser(userDto);
            studiesResponseList.add(studyResponse);
        }
        return studiesResponseList;
    }

}