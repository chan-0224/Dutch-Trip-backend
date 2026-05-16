package com.dutchtrip.dutchtrip.domain.user.service;

import com.dutchtrip.dutchtrip.domain.user.dto.UserResponse;
import com.dutchtrip.dutchtrip.domain.user.dto.UserUpdateRequest;
import com.dutchtrip.dutchtrip.domain.user.entity.User;
import com.dutchtrip.dutchtrip.domain.user.repository.UserRepository;
import com.dutchtrip.dutchtrip.global.exception.CustomException;
import com.dutchtrip.dutchtrip.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public UserResponse getMe(Long userId) {
        User user = findUser(userId);
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateBankInfo(Long userId, UserUpdateRequest request) {
        User user = findUser(userId);
        user.updateBankInfo(request.getBankName(), request.getAccountNumber());
        return UserResponse.from(user);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}
