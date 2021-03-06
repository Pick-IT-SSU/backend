package com.example.demo.user;

import com.example.demo.dto.*;
import com.example.demo.security.AuthResponse;
import com.example.demo.security.CustomUserDetails;
import com.example.demo.security.RefreshTokenRepository;
import com.example.demo.security.domain.RefreshToken;
import com.example.demo.user.domain.Company;
import com.example.demo.user.domain.Role;
import com.example.demo.user.domain.User;
import com.example.demo.user.dto.*;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.Principal;
import java.util.NoSuchElementException;
import java.util.Optional;

import static com.example.demo.user.UserDetailsServiceImpl.companyKey;

@Api(tags = {"User"})
@RestController
public class UserController {

    private UserDetailsServiceImpl userService;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    @Autowired
    public UserController(UserDetailsServiceImpl userService, BCryptPasswordEncoder bCryptPasswordEncoder){
        this.userService=userService;
        this.bCryptPasswordEncoder=bCryptPasswordEncoder;
    }

    @PostMapping("/signup")
    public ResponseEntity<ResponseMessage> signup(@RequestBody SignupDTO signupDTO){
        String pwd= signupDTO.getPassword();
        String encodedPwd=bCryptPasswordEncoder.encode(pwd);
        String imgUrl= signupDTO.getImageUrl();
        String role = signupDTO.getRole();
        if(!userService.checkEmailValidate(signupDTO.getEmail()).equals("email valid")||!userService.checkNicknameValidate(signupDTO.getNickname()).equals("nickname valid")){
            return new ResponseEntity<>(new ResponseMessage(401,"????????? ?????? ????????? ??????????????? ??????????????????."), HttpStatus.OK);
        }
        User user= new User(signupDTO.getName(), signupDTO.getNickname(), signupDTO.getEmail(),encodedPwd);

        if(imgUrl!=null){
            user.updateProfileImage(imgUrl);
        }

        if(role!=null&&role.equals(Role.ADMIN.name())) {
            user.updateUserRole();
        }

        userService.saveUser(user);
        return new ResponseEntity<>(new ResponseMessage(201,"???????????? ??????"), HttpStatus.CREATED);
    }

    @GetMapping("/signup")
    public  ResponseEntity<ResponseMessage> emailCheck(@RequestParam("email") String email){
        System.out.println("email = " + email);
        String valid=userService.checkEmailValidate(email);
        if(valid.equals("email valid")){
            return new ResponseEntity<>(new ResponseMessage(200,"????????? ????????????"), HttpStatus.OK);
        }else if(valid.equals("email github")){
            return new ResponseEntity<>(new ResponseMessage(403,"???????????? ??????????????? ??? ?????????"), HttpStatus.OK);
        }else{
            return new ResponseEntity<>(new ResponseMessage(409,"?????? ???????????? ?????????"), HttpStatus.OK);
        }
    }

    @GetMapping("/signup/{nickname}")
    public ResponseEntity<ResponseMessage> nicknameCheck(@PathVariable String nickname){
        String valid=userService.checkNicknameValidate(nickname);
        if(valid.equals("nickname valid")){
            return new ResponseEntity<>(new ResponseMessage(200,"????????? ????????????"), HttpStatus.OK);
        }
        return new ResponseEntity<>(new ResponseMessage(409,"?????? ???????????? ?????????"), HttpStatus.OK);
    }

    @GetMapping("/signup/{userId}/{nickname}")
    public ResponseEntity<ResponseMessage> nicknameUpdate(@PathVariable Long userId, @PathVariable String nickname){
        String valid=userService.checkNicknameValidate(nickname);

        if(valid.equals("nickname valid")){
            System.out.println("nickname valid");
            //user nickname update ?????? ?????? ??????
            User user=userService.findUserById(userId);
            user.setUserNickname(nickname);
            userService.saveUser(user); //update database

            user=userService.findUserById(userId);
            String newNickname=user.getUserNickname();

            return new ResponseEntity<>(new ResponseMessage(200,newNickname+" ?????? ????????? ?????? ??????! ???????????? ???????????? ????????????."), HttpStatus.OK);
        }
        return new ResponseEntity<>(new ResponseMessage(409,"?????? ???????????? ?????????"), HttpStatus.OK);
    }


    // signin??? ????????? email??? password??? ???????????? client?????? token??? return ?????????.
    @PostMapping("/signin")
    public ResponseEntity<ResponseMessage> signin(@RequestBody SigninDTO signinDTO){
        String email= signinDTO.getEmail();
        String password= signinDTO.getPassword();
        AuthResponse authResponse=userService.authenticateLogin(email,password);
        if(authResponse!=null){
            return new ResponseEntity<>(ResponseMessage.withData(200, "????????? ??????", authResponse),HttpStatus.OK);
        }

        return new ResponseEntity<>(new ResponseMessage(401,"????????? ??????"),HttpStatus.OK);
    }

    @GetMapping("/temp-login-success")
    public ResponseEntity<ResponseMessage> test(HttpServletResponse response, Principal principal) {
        String email=principal.getName();
//        System.out.println("email = " + email);
        User user=userService.findUserByEmail(email);
        Long userId=user.getUserId();

        //????????? ?????? ?????? ?????? ????????? ??????
        UserIdDto userIdDto=new UserIdDto(userId);
        System.out.println("default success url called");
        return new ResponseEntity<>(ResponseMessage.withData(200, "????????? ??????",userIdDto),HttpStatus.OK);
    }


    @PostMapping("/deploy-test")
    public ResponseEntity<ResponseMessage> deployTEST(@RequestBody SigninDTO signinDTO){
        String email= signinDTO.getEmail();
        String password= signinDTO.getPassword();
        AuthResponse authResponse=userService.authenticateLogin(email,password);
        if(authResponse!=null){
            return new ResponseEntity<>(ResponseMessage.withData(200, "????????? ??????", authResponse),HttpStatus.OK);
        }

        return new ResponseEntity<>(new ResponseMessage(401,"????????? ??????"),HttpStatus.OK);
    }

    @GetMapping("/users/{userId}/company")
    public ResponseEntity<ResponseMessage> checkUserCompany(@PathVariable Long userId, @RequestParam("email") String email){
        if(email.contains("'"))
            email=email.replace("'","");
        String domain=email.split("@")[1];
        Company company= userService.checkCompanyExistence(domain);
        if(company==null){
            return new ResponseEntity<>(new ResponseMessage(404,"???????????? ?????? ????????? ?????? ?????? ?????? ?????????."),HttpStatus.OK);
        }

        //???????????? ?????? -> ????????? ???????????? ?????? ?????????
        Boolean success=userService.sendMail(userId,email,company.getCompanyName());
        if(success){
            return new ResponseEntity<>(new ResponseMessage(200,"?????? ?????? ?????? ??????"),HttpStatus.OK);
        }else{
            return new ResponseEntity<>(new ResponseMessage(400,"?????? ?????? ?????? ??????"),HttpStatus.OK);
        }

    }

    @PostMapping("/users/{userId}/company")
    public ResponseEntity<ResponseMessage> checkCompanyEmailCertificate(@PathVariable Long userId, @RequestBody CompanyCertificateDto companyCertificateDto){
        CompanyNameKey nameAndKey = companyKey.get(userId);
        if(nameAndKey==null){
            return new ResponseEntity<>(new ResponseMessage(404,"??????????????? ?????? ??????????????????"),HttpStatus.OK);
        }
        Integer userInput=companyCertificateDto.getCertificateNumber();
        String success=userService.certificateCompanyNumber(userId,userInput,nameAndKey);
        if(success.equals("fail")){
            return new ResponseEntity<>(new ResponseMessage(200,"????????? ??????????????? ?????????????????????."),HttpStatus.OK);
        }
        companyKey.remove(userId); //???????????? ????????? ???????????? ???????????? ????????? ????????? ????????? ?????? ??????
        return new ResponseEntity<>(new ResponseMessage(201," ????????? ?????? ?????? ??????, "+success),HttpStatus.OK);

    }

    @PostMapping("/reissue")
    public ResponseEntity<AuthResponse> regenerateAccessToken(@RequestBody ReissueDto reissueDto){
        AuthResponse authResponse = userService.reissue(reissueDto.getRefreshToken());
        if(authResponse==null){
//            "?????? ???????????? ???????????? ????????? ?????? ????????? ??????"
            return new ResponseEntity<>(null,HttpStatus.OK);
        }
        return new ResponseEntity<>(authResponse,HttpStatus.OK);
    }

    @GetMapping("/logout")
    public ResponseEntity<String> logout(Principal principal, HttpServletRequest request){
        String email = principal.getName();
        userService.logout(email,request);
        return new ResponseEntity<>("???????????? ??????",HttpStatus.OK);

    }



}