package com.sungjujjang.entry.Application;

import com.sungjujjang.entry.Application.dto.GetAppliccationResponse;
import com.sungjujjang.entry.Application.dto.UpdateIntroductionRequest;
import com.sungjujjang.entry.Application.dto.UpdatePersonalInfoRequest;
import com.sungjujjang.entry.Application.dto.UpdateProposeRequest;
import com.sungjujjang.entry.Auth.dto.RegisterRequest;
import com.sungjujjang.entry.Auth.dto.RegisterResponse;
import com.sungjujjang.entry.Global.JwtAuthentication;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/application")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;
    // GET / - 현재 지원서 확인
    @GetMapping("/")
    public ResponseEntity<GetAppliccationResponse> getApplication() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(applicationService.getAppliccationResponse(authentication.getName()));
    }
    // POST /1 - 스탭 1번 실행 (없으면 생성)
    @PostMapping("/1")
    public ResponseEntity<GetAppliccationResponse> stepOne(
            @Valid @RequestBody UpdatePersonalInfoRequest updatePersonalInfoRequest
    ) {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(applicationService.updatePersonalInfo(
                    updatePersonalInfoRequest,
                    authentication.getName()
                ));
    }
    // POST /2 - 스탭 2번 실행 (없으면 생성)
    @PostMapping("/2")
    public ResponseEntity<GetAppliccationResponse> stepTwo(
            @Valid @RequestBody UpdateIntroductionRequest updateIntroductionRequest
    ) {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(applicationService.updateIntroduction(
                        updateIntroductionRequest,
                        authentication.getName()
                ));
    }
    // POST /3 - 스탭 3번 실행 (없으면 생성)
    @PostMapping("/3")
    public ResponseEntity<GetAppliccationResponse> stepThree(
            @Valid @RequestBody UpdateProposeRequest updateProposeRequest
    ) {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(applicationService.updatePropose(
                        updateProposeRequest,
                        authentication.getName()
                ));
    }
    // POST /submit - 제출
    @PostMapping("/submit")
    public ResponseEntity<GetAppliccationResponse> submitApplication() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(applicationService.submitApplication(authentication.getName()));
    }
}