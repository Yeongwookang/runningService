package com.example.runningservice.service;

import com.example.runningservice.dto.*;
import com.example.runningservice.entity.MemberEntity;
import com.example.runningservice.exception.CustomException;
import com.example.runningservice.exception.ErrorCode;
import com.example.runningservice.repository.MemberRepository;
import com.example.runningservice.util.AESUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final AESUtil aesUtil;

    @Transactional
    public MemberResponseDto registerUser(SignupRequestDto registerForm)
            throws Exception {
        // 회원가입 정보 검증
        validateRegisterForm(registerForm);

        // 사용자 엔티티 생성 및 저장(비밀번호와 전화번호는 암호화)
        MemberEntity memberEntity = registerForm.toEntity(passwordEncoder, aesUtil);
        return memberRepository.save(memberEntity).toResponseDto(aesUtil);
    }

    private void validateRegisterForm(SignupRequestDto registerForm) throws Exception {
        // 이메일 중복 체크
        if (memberRepository.existsByEmail(registerForm.getEmail())) {
            throw new CustomException(ErrorCode.ALREADY_EXIST_EMAIL);
        }
        // 회원 전화번호 중복 체크
        if (memberRepository.existsByPhoneNumber(aesUtil.encrypt(registerForm.getPhoneNumber()))) {
            throw new CustomException(ErrorCode.ALREADY_EXIST_PHONE);
        }
    }

    // 사용자 정보 조회
    public MemberResponseDto getMemberProfile(Long user_id) {
        MemberEntity memberEntity = memberRepository.findById(user_id)
            .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));

        return MemberResponseDto.of(memberEntity, aesUtil);
    }

    // 사용자 정보 수정
    @Transactional
    public MemberResponseDto updateMemberProfile(
        Long user_id, UpdateMemberRequestDto updateMemberRequestDto) {
        MemberEntity memberEntity = memberRepository.findById(user_id)
            .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));

        memberEntity.setNickName(updateMemberRequestDto.getNickName());
        memberEntity.setGender(updateMemberRequestDto.getGender());
        memberEntity.setBirthYear(updateMemberRequestDto.getBirthYear());
        memberEntity.setActivityRegion(updateMemberRequestDto.getActivityRegion());

        memberRepository.save(memberEntity);

        return MemberResponseDto.of(memberEntity, aesUtil);
    }

    // 비밀번호 변경
    @Transactional
    public void updateMemberPassword(
       Long user_id, PasswordRequestDto passwordRequestDto) {
        MemberEntity memberEntity = memberRepository.findById(user_id)
            .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));

        try {
            // 저장된 비밀번호화 입력한 oldPassword가 일치하는지 확인
            if (!passwordEncoder.matches(passwordRequestDto.getOldPassword(), memberEntity.getPassword())) {
                throw new CustomException(ErrorCode.INVALID_PASSWORD);
            }

            // 새 비밀번호 확인
            if (!passwordRequestDto.getNewPassword().equals(passwordRequestDto.getConfirmPassword())) {
                throw new CustomException(ErrorCode.INVALID_PASSWORD);
            }

            // 새 비밀번호 암호화하여 저장
            String encryptedNewPassword = aesUtil.encrypt(passwordRequestDto.getNewPassword());
            memberEntity.setPassword(encryptedNewPassword);
            memberRepository.save(memberEntity);


        } catch (Exception e) {
            throw new CustomException(ErrorCode.ENCRYPTION_ERROR);
        }
    }
    
    // 사용자 프로필 공개여부 설정
    public void updateProfileVisibility(
        Long user_id, ProfileVisibilityRequestDto profileVisibilityRequestDto) {
        MemberEntity memberEntity = memberRepository.findById(user_id)
            .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));
        
        // 프로필 공개여부 설정
    }

    // 회원 탈퇴
    public void deleteMember(Long user_id, String password) {
        MemberEntity memberEntity = memberRepository.findById(user_id)
            .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));

        try {
            // 저장된 비밀번호화 입력한 oldPassword가 일치하는지 확인
            if (!passwordEncoder.matches(password, memberEntity.getPassword())) {
                throw new CustomException(ErrorCode.INVALID_PASSWORD);
            }
            // 회원 탈퇴
            memberRepository.delete(memberEntity);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.ENCRYPTION_ERROR);
        }
    }
}
