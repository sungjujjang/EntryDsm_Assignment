package com.sungjujjang.entry.Application;

import com.sungjujjang.entry.Application.dto.*;
import com.sungjujjang.entry.Auth.User;
import com.sungjujjang.entry.Auth.UserRepository;
import com.sungjujjang.entry.Global.Errors.exception.ALREADY_SUBMITTED;
import com.sungjujjang.entry.Global.Errors.exception.APPLICATION_NOT_FOUND_ERR;
import com.sungjujjang.entry.Global.Errors.exception.BLANK_FIELD_EXIST;
import com.sungjujjang.entry.Global.Errors.exception.PHONE_DUPLICATION_ERR;
import com.sungjujjang.entry.Global.Errors.exception.USER_NOT_FOUND_ERR;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ApplicationService {
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;

    public GetAppliccationResponse getAppliccationResponse(String phone) {
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> USER_NOT_FOUND_ERR.EXCEPTION);

        ApplicationDTO data = getApplication(user);
        return GetAppliccationResponse.builder()
                .status(true)
                .data(data)
                .build();
    }

    public ApplicationDTO getApplication(User user) {
        Application application = applicationRepository.findByUser(user)
                .orElseThrow(() -> APPLICATION_NOT_FOUND_ERR.EXCEPTION);

        return ApplicationDTO.from(application);
    }

    public Boolean isSubmittedApplication(Application application) {
        return application.getSubmitedAt() != null;
    }

    public Application initBlankApplication(User user) {
        Optional<Application> application = applicationRepository.findByUser(user);

        if (application.isPresent()) {
            return application.get();
        }

        Application newApplication = Application.builder()
                .user(user).build();

        applicationRepository.save(newApplication);
        return newApplication;
    }

    public GetAppliccationResponse updatePersonalInfo(
            UpdatePersonalInfoRequest updatePersonalInfoRequest,
            String phone
    ) {
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> USER_NOT_FOUND_ERR.EXCEPTION);

        Application application = initBlankApplication(user);

        if (isSubmittedApplication(application)) {
            throw ALREADY_SUBMITTED.EXCEPTION;
        }

        application.updatePersonalInfo(updatePersonalInfoRequest);
        applicationRepository.save(application);

        ApplicationDTO data = ApplicationDTO.from(application);
        return GetAppliccationResponse.builder()
                .status(true)
                .data(data)
                .build();
    }

    public GetAppliccationResponse updateIntroduction(
            UpdateIntroductionRequest updateIntroductionRequest,
            String phone
    ) {
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> USER_NOT_FOUND_ERR.EXCEPTION);

        Application application = initBlankApplication(user);

        if (isSubmittedApplication(application)) {
            throw ALREADY_SUBMITTED.EXCEPTION;
        }

        application.updateIntroduction(updateIntroductionRequest);
        applicationRepository.save(application);

        ApplicationDTO data = ApplicationDTO.from(application);
        return GetAppliccationResponse.builder()
                .status(true)
                .data(data)
                .build();
    }

    public GetAppliccationResponse updatePropose(
            UpdateProposeRequest updateProposeRequest,
            String phone
    ) {
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> USER_NOT_FOUND_ERR.EXCEPTION);

        Application application = initBlankApplication(user);

        if (isSubmittedApplication(application)) {
            throw ALREADY_SUBMITTED.EXCEPTION;
        }

        application.updatePropose(updateProposeRequest);
        applicationRepository.save(application);

        ApplicationDTO data = ApplicationDTO.from(application);
        return GetAppliccationResponse.builder()
                .status(true)
                .data(data)
                .build();
    }

    public GetAppliccationResponse submitApplication(
            String phone
    ) {
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> USER_NOT_FOUND_ERR.EXCEPTION);

        Application application = initBlankApplication(user);

        if (isSubmittedApplication(application)) {
            throw ALREADY_SUBMITTED.EXCEPTION;
        }

        if (application.hasBlankField()) {
            throw BLANK_FIELD_EXIST.EXCEPTION;
        }

        application.setSubmitedAt();
        applicationRepository.save(application);

        ApplicationDTO data = ApplicationDTO.from(application);
        return GetAppliccationResponse.builder()
                .status(true)
                .data(data)
                .build();
    }
}
