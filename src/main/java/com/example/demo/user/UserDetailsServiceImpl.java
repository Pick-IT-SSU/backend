package com.example.demo.user;

import com.example.demo.security.AuthResponse;
import com.example.demo.security.CustomUserDetails;
import com.example.demo.security.LogoutTokenRepository;
import com.example.demo.security.RefreshTokenRepository;
import com.example.demo.security.domain.LogoutToken;
import com.example.demo.security.domain.RefreshToken;
import com.example.demo.user.domain.Company;
import com.example.demo.user.domain.User;
import com.example.demo.user.dto.CompanyNameKey;
import com.example.demo.user.repository.UserCompanyRepository;
import com.example.demo.user.repository.UserRepository;
import com.example.demo.security.jwt.JwtTokenProvider;
import com.example.demo.user.dto.SimpleUserDto;
import io.jsonwebtoken.Claims;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;


@Service
@Transactional
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;
    private final AuthenticationManager authManager;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserCompanyRepository companyRepository;
    private final JavaMailSender javaMailSender;
    public static HashMap<Long, CompanyNameKey> companyKey=new HashMap<>();
    private final RefreshTokenRepository refreshTokenRepository;
    private final LogoutTokenRepository logoutTokenRepository;

    public UserDetailsServiceImpl(UserRepository userRepository, @Lazy AuthenticationManager authManager, @Lazy BCryptPasswordEncoder bCryptPasswordEncoder, JwtTokenProvider jwtTokenProvider, UserCompanyRepository companyRepository, JavaMailSender javaMailSender, RefreshTokenRepository refreshTokenRepository, LogoutTokenRepository logoutTokenRepository) {
        this.userRepository = userRepository;
        this.authManager=authManager;
        this.bCryptPasswordEncoder=bCryptPasswordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.companyRepository = companyRepository;
        this.javaMailSender = javaMailSender;
        this.refreshTokenRepository = refreshTokenRepository;
        this.logoutTokenRepository = logoutTokenRepository;
    }

    public User findUserByEmail(String email){
        return userRepository.findByUserEmail(email); //????????? null, ????????? user ?????? return
    }

    public User findUserById(Long id){
        Optional<User> user=userRepository.findById(id);
        return user.orElse(null);
    }

    public String checkEmailValidate(String email){
        User user=userRepository.findByUserEmail(email);
        if(user==null){
            return "email valid";
        }else if(user.getLoginProvider()!=null&&user.getLoginProvider().equals("GITHUB")){
            return "email github";
        }else{
            return "email conflict";
        }
    }

    public String checkNicknameValidate(String nickname){
        User user=userRepository.findByUserNickname(nickname);
        if(user!=null){
            return "nickname conflict";
        }else{
            return "nickname valid";
        }
    }

    public Long saveUser(User user){
        return userRepository.save(user).getUserId();
    }

    public AuthResponse authenticateLogin(String email, String pwd){
        User user=userRepository.findByUserEmail(email);
        if(user==null){
            return null;
        }
        if(user.getLoginProvider()==null&&!bCryptPasswordEncoder.matches(pwd,user.getUserPassword())){
            return null;
        }

        //Authentication Token ?????? (username, password) ??????
        //????????? username: ???????????? ?????? ????????? -> email ??? ???????????? ??????
//        UsernamePasswordAuthenticationToken authToken=new UsernamePasswordAuthenticationToken(email,pwd);
//        Authentication auth=authManager.authenticate(authToken);
//        SecurityContextHolder.getContext().setAuthentication(auth);
        String refreshToken = jwtTokenProvider.createAndSaveRefreshToken(user);
        String accessToken = jwtTokenProvider.createAccessToken(user);
        return new AuthResponse(accessToken,user.getUserId(),refreshToken);

    }

    public void deleteUserById(Long userId){
        userRepository.deleteByUserId(userId);
    }


    //isThisUserWriter ?????? ????????? ?????? ?????? ???????????????
    public SimpleUserDto getSimpleUserDto(User user){
        SimpleUserDto simpleUserDto=new SimpleUserDto();
        BeanUtils.copyProperties(user,simpleUserDto);
        return simpleUserDto;
    }

    public Company checkCompanyExistence(String domain){
        return companyRepository.findByCompanyDomain(domain);
    }

    public String checkUserCompanyStatus(Long userId){
        User user=userRepository.findById(userId).get();
        String requestStatus=user.getUserCompany();
        if(requestStatus==null){
            return "ask";
        }else if(requestStatus.equals("NO_REQUEST")){
            return "no request";
        }else{
            return requestStatus;
        }
    }

    public Boolean sendMail(Long userId,String email,String companyName){
        Boolean success=false;
        try{
            SimpleMailMessage message=new SimpleMailMessage();
            Integer randomKey= ThreadLocalRandom.current().nextInt(1000,10000); //?????? ????????????
            message.setTo(email);
            message.setSubject("[PICK-IT] ???????????? ???????????????.");
            message.setText("?????? ????????? ??????????????? ?????? ???????????? 4????????? ???????????? ??????????????????.\n"+randomKey);
            javaMailSender.send(message);
            success=true;
            //??????????????? ????????? ?????? ??????, companyKey??? ?????? ????????? ???????????? ????????? ?????? ????????? put
            companyKey.remove(userId);
            companyKey.put(userId,new CompanyNameKey(randomKey,companyName));
        }catch (MailException e){
            e.printStackTrace();
        }
        return success;
    }

    public String certificateCompanyNumber(Long userId, Integer userInput,CompanyNameKey nameAndKey){
        String companyName=nameAndKey.getCompanyName();
        Integer randomKey=nameAndKey.getRandomKey();
        if(randomKey.equals(userInput)){
            User user = userRepository.findById(userId).get();
            user.updateUserCompany(companyName);
            userRepository.save(user);
            return companyName;
        }else{
            return "fail";
        }
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        //spring security?????? ????????? ??? ???????????? ????????? ??? ???????????? ?????? ?????????, security context??? ????????????!
        User user=userRepository.findByUserEmail(email);
        return new CustomUserDetails(user);
        //CustomUserDetails??? Authentication type?????? ??????????????? ???????????? ??????
        //User Entity ????????? authority ??? ?????? ?????? ?????? ??????
    }

    public AuthResponse reissue(String refreshToken){
        Claims claims = jwtTokenProvider.parseToken(refreshToken);
        String email = (String) claims.get("email");
        RefreshToken foundToken = refreshTokenRepository.findById(email).orElse(null);
        if(foundToken==null)
            return null;

        if(refreshToken.equals(foundToken.getRefreshToken())) {
            User user=this.findUserByEmail(email);
            String newAccessToken=jwtTokenProvider.createAccessToken(user);
            String newRefreshToken=reissueRefreshToken(refreshToken,user);
            return new AuthResponse(newAccessToken,user.getUserId(),newRefreshToken);
        }
        return null;
    }

    private String reissueRefreshToken(String refreshToken, User user) {
        String newRefreshToken = null;
        if(jwtTokenProvider.remainingTimeInToken(refreshToken)<1000L * 60 * 60 * 24 * 3){
            newRefreshToken = jwtTokenProvider.createAndSaveRefreshToken(user);
        }
        return newRefreshToken;

    }

    public void logout(String email, HttpServletRequest request){
        String accessToken = jwtTokenProvider.getJwtTokenFromRequestHeader(request);
        refreshTokenRepository.deleteById(email);
        Long remainingTimeInToken = jwtTokenProvider.remainingTimeInToken(accessToken);
        logoutTokenRepository.save(LogoutToken.of(accessToken,email,remainingTimeInToken));

    }
}
