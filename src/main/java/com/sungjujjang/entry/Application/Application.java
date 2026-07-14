package com.sungjujjang.entry.Application;

import com.sungjujjang.entry.Application.dto.UpdateIntroductionRequest;
import com.sungjujjang.entry.Application.dto.UpdatePersonalInfoRequest;
import com.sungjujjang.entry.Application.dto.UpdateProposeRequest;
import com.sungjujjang.entry.Auth.User;
import com.sungjujjang.entry.Global.Enums.Gender;
import com.sungjujjang.entry.Global.Enums.Region;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Application {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String birth;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    private Region region;

    @Column(length = 2000)
    private String introduction;

    @Column(length = 2000)
    private String propose;

    private LocalDateTime submitedAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    public void updatePersonalInfo(UpdatePersonalInfoRequest dto) {
        if (dto.name() != null) this.name = dto.name();
        if (dto.birth() != null) this.birth = dto.birth();
        if (dto.gender() != null) this.gender = dto.gender();
        if (dto.region() != null) this.region = dto.region();
    }

    public void updateIntroduction(UpdateIntroductionRequest updateIntroductionRequest) {
        this.introduction = updateIntroductionRequest.introduction();
    }

    public void updatePropose(UpdateProposeRequest updateProposeRequest) {
        this.propose = updateProposeRequest.propose();
    }

    public void setSubmitedAt() {
        this.submitedAt = LocalDateTime.now();
    }
}
