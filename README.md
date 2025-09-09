# KakaoBase 백엔드

## 프로젝트 소개
Java와 Spring을 기반으로, 실시간 상호작용에 중점을 둔 SNS 백엔드 플랫폼입니다.
  채팅, 알림, 팔로우 등 다양한 기능을 통해 사용자 경험을 풍부하게 하고, 안정적인 대규모 트래픽 처리를 목표로 설계되었습니다.
  이벤트 기반 아키텍처와 비동기 처리, 그리고 Redis를 활용한 캐싱 전략을 도입하여 시스템의 응답 속도와 처리량을 최적화 하였습니다.

## 팀 위키
- [팀 위키](https://github.com/100-hours-a-week/22-tenten-wiki/wiki)
- [백엔드 위키](https://github.com/100-hours-a-week/22-tenten-wiki/wiki/Backend-Wiki)
- [시연영상](https://www.youtube.com/shorts/WbWPg2TR-cw)

## 목차 📑
- [기술 스택 🛠️](#기술-스택)
- [프로젝트 구조 📂](#프로젝트-구조-)
- [주요 기능 ✨](#주요-기능-)
- [프로젝트 관리 📊](#프로젝트-관리-)
- [팀원 👨‍💻👩‍💻](#팀원-)

## 기술 스택 🛠️
  
  ### Core
   - 언어: Java 21
   - 프레임워크: Spring Boot 3.4.5
   - 빌드 도구: Gradle
   - API 문서화: SpringDoc (Swagger), [Google Sheet](//링크)

  ### 데이터베이스 및 캐싱
   - RDBMS: MySQL 8.0
   - ORM: Spring Data JPA
   - 캐싱: Redis (Spring Data Redis, Redisson)
   - 쿼리 도구: QueryDSL

  ### 인증 및 보안
   - 인증/인가: JWT 기반의 자체 인증/인가
   - 보안: Spring Security 6.x
   - 토큰 라이브러리: JJWT 0.11.5

###   실시간 통신
   - WebSocket: Spring WebSocket
   - 보안: Spring Security Messaging

  ### 테스트 및 품질 관리
   - 테스트 프레임워크: JUnit 5, Mockito, AssertJ
   - 통합 테스트: Testcontainers
   - API 테스트: MockMvc

  ### 인프라 및 배포
   - 컨테이너화: Docker
   - CI/CD: GitHub Actions
   - 클라우드: AWS (EC2, S3, RDS, ElastiCache)

###   기타 도구
   - 모니터링: Prometheus, Spring Boot Actuator
   - 로깅: Logback, p6spy
   - 파일 저장소: AWS S3

##  주요 기능 ✨

  ### 사용자 및 인증 도메인
   - 이메일 기반 회원가입 및 사용자 프로필 관리
   - JWT (Access/Refresh Token) 기반의 로그인 및 인증/인가 시스템
   - 사용자 검색 기능
   - 이메일 인증을 통한 계정 활성화

  ### 게시물 도메인
   - S3를 연동한 이미지 업로드를 포함한 게시물 작성, 조회, 수정, 삭제
   - 게시물 목록 조회 (피드)
   - 게시물 상세 조회
   - 게시물 좋아요 기능

  ### 댓글 도메인
   - 특정 게시물에 대한 댓글 작성 및 삭제
   - 게시물별 댓글 목록 조회 (페이지네이션)
   - 댓글 작성자 정보 표시

  ### 팔로우 도메인
   - 다른 사용자 팔로우 및 언팔로우 기능
   - 특정 유저의 팔로워 및 팔로잉 목록 조회

  ### 실시간 채팅 도메인
   - WebSocket 기반의 실시간 1:1 채팅 기능
   - 채팅방 생성 및 목록 조회
   - 이전 대화 내용 조회

  ### 알림 도메인
   - WebSocket을 통한 실시간 알림 기능 (새 팔로우, 게시물 좋아요, 새 댓글 등)
   - 수신된 알림 목록 조회
   - 알림 읽음 처리 기능

## 프로젝트 관리 📊

### 이슈 관리
- GitHub Issues를 활용한 이슈 추적
- 기능 단위 브랜치 관리 (main, develop, feature)
- 코드 리뷰 프로세스를 통한 품질 관리

### 배포 프로세스
- GitHub Actions를 통한 CI/CD 파이프라인
- 개발(dev),  프로덕션(prod) 환경 분리

## 팀원 👨‍💻👩‍💻
| 이름 | 역할 | 주요 업무 |
|------------------|------|------------|
| hazel.kim (김희재) | 팀장/클라우드 | • AWS 운영 및 관리<br>• 부하테스트 진행 및 개선 |
| rick.lee (이강협) | 백엔드 | • 백엔드 시스템 설계 및 개발<br>• API 설계 및 구현<br>• 데이터베이스 모델링 |
| daisy.kim (김도현) | 프론트엔드 | • 프론트엔드 개발<br>• UI/UX 설계<br>• 사용자 인터페이스 구현 |
| astra.ka (가을) | 인공지능 | • AI 소셜봇 기능 개발<br>• 파인 튜닝 |
| dobby.choi (최우성) | 인공지능 | • AI 유튜브 요약 기능 개발<br>• LangChain |
| marchello.lee (이정민) | 클라우드 | • GCP 운영 및 관리<br>• 모니터링 구축 |
