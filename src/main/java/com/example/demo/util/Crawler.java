package com.example.demo.util;

import com.example.demo.hashtag.Hashtag;
import com.example.demo.hashtag.service.HashtagService;
import com.example.demo.lecture.Lecture;
import com.example.demo.lecture.LectureService;
import com.example.demo.lecture.RequestedLecture;
import com.example.demo.lecture.repository.RequestedLectureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Slf4j
@Component
public class Crawler {
    private final HashtagService hashtagService;
    private final LectureService lectureService;
    private final RequestedLectureRepository requestedLectureRepository;

    public Crawler(HashtagService hashtagService, @Lazy LectureService lectureService, RequestedLectureRepository requestedLectureRepository) {
        this.hashtagService = hashtagService;
        this.lectureService = lectureService;
        this.requestedLectureRepository = requestedLectureRepository;
    }

    public static void main(String[] args) {
//        udemy("https://www.udemy.com/course/clean-code-js");
//        fastcampus("https://fastcampus.co.kr/dev_academy_kmt3");
//        inflearn("https://www.inflearn.com/course/%EC%A0%95%EB%8C%80%EB%A6%AC-%EC%8A%A4%EC%9C%84%ED%94%84%ED%8A%B8-%EA%B8%B0%EC%B4%88");
//        youtube("https://www.youtube.com/watch?v=6s51_S3aols");
//        nomadcoders("https://nomadcoders.co/nomadcoin");

        //1. url 중복 검증
        //2. url 에 들어있는 사이트 이름 식별 -> 크롤링 요청
        //3. 크롤링 요청 내부에서 saveLecture, manageHashtags 수행 (saveRequiredLecture 메소드 해당 파일 하단에 있음)
    }

    /**
     *
     * 파라미터의 lectureId == requestedLectureId (등록 요청 강의 status 변경 위해)
     * HashtagController 에서 테스트로 호출하면 lectureId가 null 로 넘어옴
     * lectureId == null 이면 테스트용으로 판단, 디비에 저장하는 작업 거치지 않음
     */
    @Async
    public void inflearn(String url, Long lectureId){
        long start = System.currentTimeMillis();
        log.info("start - "+start+" - "+Thread.currentThread().getName());
        Document document;

        try {
            document = Jsoup.connect(url).get();

        } catch (Exception e) {
            e.printStackTrace();
            if(lectureId!=null){
                changeRequestedLectureStatus(lectureId,"error");
            }
            return;
            //잘못된 url 연결 error throw
        }

        List<String> hashtags=new ArrayList<>();
        String img;
        String title;
        String lecturer;

        try{
            img = document.selectFirst("div.cd-header__thumbnail img").attr("src");

            title = document.selectFirst("div.cd-header__title").text();

            lecturer = document.selectFirst("a.cd-header__instructors--main").text();

            Elements elements = document.select("a.cd-header__tag");
            for(Element e:elements){
                String tag=e.text();
                hashtags.add(tag);
            }
        }catch (Exception e){
            e.printStackTrace();
            changeRequestedLectureStatus(lectureId,"error");
            return;
        }



        if(lectureId==null){
            return;
        }

        Lecture lecture = Lecture.builder()
                .lecturer(lecturer)
                .lectureUrl(url)
                .lectureTitle(title)
                .thumbnailUrl(img)
                .siteName("인프런")
                .build();

        saveRequiredLecture(lecture,hashtags,lectureId);
        long end = System.currentTimeMillis();
        log.info("end - "+end+" - "+Thread.currentThread().getName());
        log.info("diff inflearn - "+(end-start)+" - "+Thread.currentThread().getName());
    }

    @Async
    public void youtube(String url, Long lectureId){
        long start=System.currentTimeMillis();
        log.info("start - "+start+" - "+Thread.currentThread().getName());
        Document document;

        try {
            document = Jsoup.connect(url).get();

        } catch (Exception e) {
            e.printStackTrace();
            if(lectureId!=null){
                changeRequestedLectureStatus(lectureId,"error");
            }
            return;
            //잘못된 url 연결 error throw
        }

//        log.info("after document - "+(System.currentTimeMillis()-start));
        List<String> tags=new ArrayList<>();
        Element body = document.body();
        String title;
        String content;
        String lecturer;
        String img;

        try {
            title = document.head().selectFirst("meta[property=og:title]").attr("content");
            content = document.head().selectFirst("meta[property=og:description]").attr("content");
            lecturer = body.selectFirst("div#watch7-content link[itemprop=name]").attr("content");
            img = document.head().selectFirst("meta[property=og:image]").attr("content");


        }catch (Exception e){
            e.printStackTrace();
            changeRequestedLectureStatus(lectureId,"error");
            return;
        }

        if (title.contains("#")) {
            String[] split = title.split("#");
            //0번 idx 이후부터는 전부 다 해시태그임
            for (int i = 1; i < split.length; i++) {
                String replace = split[i].replace(" ", "");
                tags.add(replace);
                if (tags.size() == 3)
                    break;
            }
        }

        if(tags.size()<3){
            List<String> hashtagsInTitle = findHashtagsInTitle(title, 5);
            tags.addAll(hashtagsInTitle.stream()
                    .filter(h->!tags.contains(h))
                    .limit(3-tags.size())
                    .collect(Collectors.toList()));
        }

        if(tags.size()<3){
            List<String> hashtagsInTitle = findHashtagsInTitle(content, 5);
            tags.addAll(hashtagsInTitle.stream()
                    .filter(h->!tags.contains(h))
                    .limit(3-tags.size())
                    .collect(Collectors.toList()));
        }

        if(lectureId==null){
            return;
        }

        Lecture lecture = Lecture.builder()
                .lectureTitle(title)
                .siteName("유튜브")
                .thumbnailUrl(img)
                .lectureUrl(url)
                .lecturer(lecturer)
                .build();

        saveRequiredLecture(lecture,tags,lectureId);

        long end=System.currentTimeMillis();
        log.info("end - "+end+" - "+Thread.currentThread().getName());
        log.info("diff youtube - "+(end-start)+" - "+Thread.currentThread().getName());

    }

    @Async
    public void nomadcoders(String url, Long lectureId){
        long start=System.currentTimeMillis();
        log.info("start - "+start+" - "+Thread.currentThread().getName());
        String baseUrl="https://nomadcoders.co/courses";

        Document document;
        Document base;

        try {
            document = Jsoup.connect(url).get();
            base=Jsoup.connect(baseUrl).get();

        } catch (Exception e) {
            e.printStackTrace();
            if(lectureId!=null){
                changeRequestedLectureStatus(lectureId,"error");
            }
            return;
            //잘못된 url 연결 error throw
        }

        String title;
        String content;
        Elements div;
        String lecturer="니꼴라스";
        String siteName="노마드코더";
        String imgUrl=null;
        List<String> tags=new ArrayList<>();


        try {

            title = document.head().selectFirst("meta[property=og:title]").attr("content");
            String[] split = title.split("노마드 코더");
            title = split[0].substring(0, split[0].length() - 3);
            content = document.head().selectFirst("meta[property=og:description]").attr("content");
            div = base.select("div.sc-7257b669-0.kKJInu.flex.flex-col.relative.rounded-lg.items-center");

            String[] split1 = url.split("/");
            String last = split1[split1.length - 1];

            for(Element e:div){
                if(e.selectFirst("a").attr("href").contains(last)){
                    Elements img = e.getElementsByTag("img");
                    imgUrl="https://nomadcoders.co"+img.get(1).attr("src");
                    break;
                }
            }

        }catch (Exception e){
            e.printStackTrace();
            changeRequestedLectureStatus(lectureId,"error");
            return;
        }


        if(content.contains(",")){
            Arrays.stream(content.split(","))
                    .limit(3)
                    .forEach(t->tags.add(t.replace(" ","")));
        }else if(content.contains("+")){
            Arrays.stream(content.split("\\+"))
                    .limit(3)
                    .forEach(t->tags.add(t.replace(" ","")));
        }else{
            tags.addAll(findHashtagsInTitle(title,3));  //제목에서도 찾고
            if(tags.size()<3){
                tags.addAll(findHashtagsInTitle(content,10)
                        .stream()
                        .filter(h-> !tags.contains(h))
                        .limit(3-tags.size())
                        .collect(Collectors.toList()));
            }

        }

        if(lectureId==null){
            return;
        }

        Lecture lecture = Lecture.builder()
                .lectureTitle(title)
                .siteName(siteName)
                .thumbnailUrl(imgUrl)
                .lectureUrl(url)
                .lecturer(lecturer)
                .build();

        saveRequiredLecture(lecture,tags,lectureId);

        long end=System.currentTimeMillis();
        log.info("end - "+end+" - "+Thread.currentThread().getName());
        log.info("diff nomad - "+(end-start)+" - "+Thread.currentThread().getName());

    }

    @Async
    public void spartaCoding(String url, Long lectureId){
        long start=System.currentTimeMillis();
        log.info("start - "+start+" - "+Thread.currentThread().getName());
        Document document;

        try {
            document = Jsoup.connect(url).get();
        } catch (Exception e) {
            e.printStackTrace();
            if(lectureId!=null){
                changeRequestedLectureStatus(lectureId,"error");
            }
            return;
            //잘못된 url 연결 error throw
        }
        String siteName="스파르타코딩클럽";
        String lecturer="스파르타코딩클럽";
        log.info("after sparta document - "+(System.currentTimeMillis()-start));

        String title;
        String img;
        List<String> hashtags;
        try {
            String keywords = document.head().selectFirst("meta[name=keywords]").attr("content");
            //keywords 가 null 이라면? description 가져오기
            if (keywords.length() < 1) {
                keywords = document.head().selectFirst("meta[property=og:description]").attr("content");
            }

            hashtags = findHashtagsInTitle(keywords, 3);

            title = document.head().selectFirst("title").text();
            String[] split = title.split("\\|");
            title = split[1].replaceFirst(" ", "");


            String imageUrl = document.head().selectFirst("meta[property=og:image]").attr("content");
            img = "https://spartacodingclub.kr" + imageUrl;

        }catch (Exception e){
            e.printStackTrace();
            changeRequestedLectureStatus(lectureId,"error");
            return;
        }


        if(lectureId==null){
            return;
        }

        Lecture lecture = Lecture.builder()
                .lectureTitle(title)
                .siteName(siteName)
                .thumbnailUrl(img)
                .lectureUrl(url)
                .lecturer(lecturer)
                .build();

        saveRequiredLecture(lecture,hashtags,lectureId);

        long end=System.currentTimeMillis();
        log.info("end - "+end+" - "+Thread.currentThread().getName());
        log.info("diff sparta - "+(end-start)+" - "+Thread.currentThread().getName());

    }

    @Async
    public void projectlion(String url, Long lectureId){
        long start=System.currentTimeMillis();
        log.info("start - "+start+" - "+Thread.currentThread().getName());
        Document document;

        try {
            document = Jsoup.connect(url).get();
        } catch (Exception e) {
            e.printStackTrace();
            if(lectureId!=null){
                changeRequestedLectureStatus(lectureId,"error");
            }
            return;
            //잘못된 url 연결 error throw
        }

        String title;
        String finalTitle;
        String image;
        String desc;

        try {
            title = document.head().selectFirst("meta[property=og:title]").attr("content");
            int index = title.indexOf(":");
            finalTitle = title.substring(1, index - 1);
            image = document.head().selectFirst("meta[property=og:image]").attr("content");
            desc = document.head().selectFirst("meta[property=og:description]").attr("content");

        } catch (Exception e){
            e.printStackTrace();
            changeRequestedLectureStatus(lectureId,"error");
            return;
        }

        String lecturer = "프로젝트 라이언";
        String siteName = "Project lion";

        /**
         * 이거 갑자기 입문 왜안나오는지 확인 필요
            - UX/ UI 입문자를 위한 UX Discovery 인 경우에 R / ux / ui 가 나옴
            - 순서대로 나와서 자르다보니까 '입문' 이 나머지 3개보다 뒷번호라서 안나옴
            -> 그 강의를 잘 설명하는 해시태그가 안나올 수 있다는 한계점 아주..아주..
         */
        // 제목에서 해시태그 추출
        List<String> hashtags = findHashtagsInTitle(title, 3);

        // 제목추출 이후 description 에서 채움
        if(hashtags.size()<3){
            List<String> hashtagsInDesc = findHashtagsInTitle(desc, 3 - hashtags.size());
            hashtags.addAll(hashtagsInDesc);
        }

        if(lectureId==null){
            return;
        }

        Lecture lecture = Lecture.builder()
                .lecturer(lecturer)
                .lectureUrl(url)
                .lectureTitle(title)
                .thumbnailUrl(image)
                .siteName(siteName)
                .build();

        saveRequiredLecture(lecture,hashtags,lectureId);
        long end=System.currentTimeMillis();
        log.info("end - "+end+" - "+Thread.currentThread().getName());
        log.info("diff projectLion - "+(end-start)+" - "+Thread.currentThread().getName());

    }

    @Async
    public void udemy(String url, Long lectureId){
        long start=System.currentTimeMillis();
        log.info("start - "+start+" - "+Thread.currentThread().getName());
        Document document;

        try {
            document = Jsoup.connect(url).userAgent("Mozilla/5.0").get();
        } catch (Exception e) {
            e.printStackTrace();
            if(lectureId!=null){
                changeRequestedLectureStatus(lectureId,"error");
            }
            return;
            //잘못된 url 연결 error throw
        }
        log.info("after udemy document - "+(System.currentTimeMillis()-start));

        String lecturer;
        String title;
        String image;
        String siteName = "udemy";
        List<String> hashtags;
        try {
            title = document.head().selectFirst("meta[name=title]").attr("content");
            image = document.head().selectFirst("meta[property=og:image]").attr("content");
            lecturer = document
                    .selectFirst("a.udlite-btn.udlite-btn-large.udlite-btn-link.udlite-heading-md.udlite-text-sm.udlite-instructor-links span")
                    .text();

            // 총 해시태그 담는 곳
            // 제목에서 해시태그 추출
            hashtags = findHashtagsInTitle(title, 3);

            //클린코드 자바스크립트인 경우 -> 자바, 자바 스크립트 출력

            // 제목에 3개 없으면 카테고리에서 추출
            /**
             * 개발 > 웹 개발 > JavaScript
             * 이런식으로 되어있어서 소분류부터 거꾸로 넣었는데 괜찮은가요..
             */
            if (hashtags.size() < 3) {
                Elements elements = document.select("a.udlite-heading-sm");
                for (int i = elements.size() - 1; i >= 0; i--) {
                    if (hashtags.size() >= 3)
                        break;
                    String tag = elements.get(i).text();

                    // hashtags에 없는 경우에만 담기
                    if (!hashtags.contains(tag))
                        hashtags.add(tag);
                }
            }
        } catch (Exception e){
            e.printStackTrace();
            changeRequestedLectureStatus(lectureId,"error");
            return;
        }

        if(lectureId==null){
            return;
        }

        Lecture lecture = Lecture.builder()
                .lecturer(lecturer)
                .lectureUrl(url)
                .lectureTitle(title)
                .thumbnailUrl(image)
                .siteName(siteName)
                .build();


        saveRequiredLecture(lecture,hashtags,lectureId);
        long end=System.currentTimeMillis();
        log.info("end - "+end+" - "+Thread.currentThread().getName());
        log.info("diff udemy - "+(end-start)+" - "+Thread.currentThread().getName());

    }

    @Async
    public void fastcampus(String url, Long lectureId){
        long start=System.currentTimeMillis();
        log.info("start - "+start+" - "+Thread.currentThread().getName());
        Document document;

        try {
            document = Jsoup.connect(url).get();
        } catch (Exception e) {
            e.printStackTrace();
            if(lectureId!=null){
                changeRequestedLectureStatus(lectureId,"error");
            }
            return;
            //잘못된 url 연결 error throw
        }

        String title;
        String desc;
        String finalTitle;
        String image;
        String lecturer = "패스트 캠퍼스";
        String siteName = "패스트캠퍼스";

        try {
            title = document.head().selectFirst("meta[property=og:title]").attr("content");
            int index = title.indexOf("|");
            finalTitle = title.substring(1, index - 1);
            image = document.selectFirst("p.container__text-content.fc-h1-text").selectFirst("img").attr("src");


            desc = document.head().selectFirst("meta[name=description]").attr("content");
        } catch (Exception e){
            e.printStackTrace();
            changeRequestedLectureStatus(lectureId,"error");
            return;
        }

        // 제목 추출
        List<String> hashtags = findHashtagsInTitle(title, 3);

        // 제목추출 이후 description 에서 채움
        if(hashtags.size()<3){
            List<String> hashtagsInDesc = findHashtagsInTitle(desc, 3 - hashtags.size());
            hashtags.addAll(hashtagsInDesc);
        }

        if(lectureId==null){
            return;
        }

        Lecture lecture = Lecture.builder()
                .lecturer(lecturer)
                .lectureUrl(url)
                .lectureTitle(finalTitle)
                .thumbnailUrl(image)
                .siteName(siteName)
                .build();

        saveRequiredLecture(lecture,hashtags,lectureId);

        long end=System.currentTimeMillis();
        log.info("end - "+end+" - "+Thread.currentThread().getName());
        log.info("diff fast campus - "+(end-start)+" - "+Thread.currentThread().getName());

    }

    @Async
    public void codingEverybody(String url, Long lectureId){
        long start=System.currentTimeMillis();
        log.info("start - "+start+" - "+Thread.currentThread().getName());
        Document document;

        int lastSlash = url.lastIndexOf("/");
        String mainUrl = url.substring(0, lastSlash);
        try {
            document = Jsoup.connect(mainUrl).get();
        } catch (Exception e) {
            e.printStackTrace();
            if(lectureId!=null){
                changeRequestedLectureStatus(lectureId,"error");
            }
            return;
            //잘못된 url 연결 error throw
        }

        String lecturer = "egoing";
        String img = "";
        String siteName = "생활코딩";
        String title;
        String finalTitle;
        List<String> hashtags;

        try {
            title = document.head().selectFirst("meta[property=og:title]").attr("content");
            finalTitle = title.substring(0, title.length() - 7);

            // 총 해시태그 담는 곳
            hashtags = findHashtagsInTitle(title, 3);

            // 토픽목록에서 추출
            if (hashtags.size() < 3) {
                Elements elements = document.select("div.label");

                for (Element e : elements) {
                    if (hashtags.size() < 3) {
                        String keyword = e.selectFirst("a").selectFirst("span").text();
                        if (keyword != "") {
                            hashtags.addAll(findHashtagsInTitle(keyword, 3 - hashtags.size()));
                        }
                    } else
                        break;
                }
            }
        } catch (Exception e){
            e.printStackTrace();
            changeRequestedLectureStatus(lectureId,"error");
            return;
        }


        if(lectureId==null){
            return;
        }

        Lecture lecture = Lecture.builder()
                .lecturer(lecturer)
                .lectureUrl(url)
                .lectureTitle(finalTitle)
                .thumbnailUrl(img)
                .siteName(siteName)
                .build();

        saveRequiredLecture(lecture,hashtags,lectureId);
        long end=System.currentTimeMillis();
        log.info("end - "+end+" - "+Thread.currentThread().getName());
        log.info("diff open tutorials- "+(end-start)+" - "+Thread.currentThread().getName());
    }

    private List<String> findHashtagsInTitle(String title, int needCount){
        return hashtagService.getAllHashtags()
                .stream()
                .filter(hashtag ->
                    title.toLowerCase().contains(hashtag.getHashtagName().toLowerCase())
                )
                .limit(needCount)
                .map(Hashtag::getHashtagName) // ContainingClass::methodName
                .collect(Collectors.toList());
    }

    private void saveRequiredLecture(Lecture lecture, List<String> hashtags,Long requestedLectureId){
        if(requestedLectureId==null){
            return;
        }
        lectureService.saveLecture(lecture);
        lectureService.manageHashtag(hashtags, lecture);
        changeRequestedLectureStatus(requestedLectureId,"success");
    }

    //크롤링 성공하고 나면 requestedLecture 에 managedStatus 1로 변경 -> 등록 완료로 표시!
    //크롤링 과정에서 오류나면 requestedLecture status -1 로 변경
    private void changeRequestedLectureStatus(Long requestedLectureId, String status){
        System.out.println("requestedLectureId = " + requestedLectureId + ", status = " + status);
        if(requestedLectureId==null){
            return;
        }
        Optional<RequestedLecture> requestedLecture = requestedLectureRepository.findById(requestedLectureId);

        if(requestedLecture.isPresent()){
            RequestedLecture foundLecture=requestedLecture.get();
            foundLecture.modifyManagedStatus(status.equals("success")?1:-1); //성공이면 1, error 면 -1
            requestedLectureRepository.save(foundLecture);
        }
    }



}
