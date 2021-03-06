package com.example.demo.userPreferenceHashtag;

import com.example.demo.hashtag.Hashtag;
import com.example.demo.user.domain.User;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "userPreferenceHashtags")
@NoArgsConstructor
public class UserPreferenceHashtag {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userPreferenceTagId;

    //1 hashtag -> N 선호도 tag
    @ManyToOne
    @JoinColumn(name = "hashtagId")
    private Hashtag hashtag;

    //한 명의 사용자에 대해 여러 선호도 태그그
    @ManyToOne
    @JoinColumn(name = "userId")
    private User user;

    @Column(nullable = false)
    private Integer preference=1;

    @Column(nullable = false)
    private LocalDateTime lastLiked;

    @Builder
    public UserPreferenceHashtag(Hashtag hashtag, User user){
        this.lastLiked=LocalDateTime.now();
        this.hashtag=hashtag;
        this.user=user;
    }

    public void updatePreference(int likeStatus){
        if(likeStatus==1){
            this.lastLiked=LocalDateTime.now();
        }
        this.preference+=likeStatus;
    }
}
