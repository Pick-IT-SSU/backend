package com.example.demo.controller;

import com.example.demo.domain.StudyComment;
import com.example.demo.domain.StudyPost;
import com.example.demo.domain.User;
import com.example.demo.dto.ResponseMessage;
import com.example.demo.dto.StudyCommentDto;
import com.example.demo.security.UserDetailsServiceImpl;
import com.example.demo.service.StudyCommentService;
import com.example.demo.service.StudyPostService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;

@RestController
@AllArgsConstructor
@Transactional
public class StudyCommentController {
    private final StudyCommentService studyCommentService;
    private final UserDetailsServiceImpl userDetailsService;
    private final StudyPostService studyPostService;

    @PostMapping("/studies/{studyId}/comments")
    public ResponseEntity<ResponseMessage> addStudyComment(@PathVariable Long studyId, @RequestBody StudyCommentDto commentDto, Principal principal){
        String email=principal.getName();
        User user=userDetailsService.findUserByEmail(email);

        Long parentId=commentDto.getCommentParentId(); // 0이면 부모의 댓글이 온 것, 그 외면 얘의 부모 댓글이 온 것
        Integer classId=commentDto.getCommentClass(); // 0이면 부모댓글, 1이면 자식댓글
        String content=commentDto.getCommentContent();
        StudyPost studyPost=studyPostService.findStudyPostById(studyId);

        StudyComment comment=new StudyComment(content,classId);
        comment.setStudyPost(studyPost);
        comment.setUser(user);
        Long generatedId=studyCommentService.saveStudyComment(comment);

        comment.updateGroupId(parentId==0?generatedId:parentId); // parentId==0이면, 방금 등록한 애가 원댓글 -> 걔의 id가 그대로 groupId / 0이 아니면? parentId값 찾아서 등록

        studyCommentService.saveStudyComment(comment);
        return new ResponseEntity<>(ResponseMessage.withData(201,"스터디 댓글이 등록 되었습니다.",comment), HttpStatus.OK);

    }

    @PatchMapping("/studies/{studyId}/comments/{commentId}")
    public ResponseEntity<ResponseMessage> modifyStudyComment(@PathVariable Long studyId, @PathVariable Long commentId,@RequestBody HashMap<String, String> params){
        //댓글을 수정하면 studyPost에 있는 리스트도 같이 업데이트 되는지 확인해보기
        String content=params.get("content");
        StudyComment comment=studyCommentService.modifyStudyComment(content,commentId);
        if(comment==null) {
            return new ResponseEntity<>(new ResponseMessage(404, "잘못된 댓글에 대한 수정 요청입니다."), HttpStatus.OK);
        }else{
            return new ResponseEntity<>(ResponseMessage.withData(200,"스터디 댓글이 수정되었습니다.",comment), HttpStatus.OK);
        }
    }

    @DeleteMapping("/studies/{studyId}/comments/{commentId}")
    public ResponseEntity<ResponseMessage> deleteStudyComment(@PathVariable Long commentId){
        StudyComment comment=studyCommentService.findStudyCommentById(commentId);
        if(comment==null){
            return new ResponseEntity<>(new ResponseMessage(404, "존재하지 않는 댓글에 대한 요청입니다."), HttpStatus.OK);
        }else{
            comment.setCommentStatus(0); // 삭제된 글로 상태 변경
            studyCommentService.saveStudyComment(comment);
            return new ResponseEntity<>(new ResponseMessage(200,commentId+"번 댓글 삭제 성공"),HttpStatus.OK);
        }
    }

}
