package com.indorcallejero.api.user;

import com.indorcallejero.api.auth.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// @Transactional a nivel de clase, agregado en la Etapa 12: UserDTO ahora
// expone roles (@ElementCollection LAZY en UserEntity). Antes de esto ningún
// método tocaba esa colección, así que no hacía falta -- justo el patrón de
// LazyInitializationException que ya se repitió en las Etapas 2 y 6.
@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserService(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    @Transactional(readOnly = true)
    public Page<UserDTO> getUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(userMapper::toDto);
    }

    @Transactional(readOnly = true)
    public UserDTO getUserById(Long id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        return userMapper.toDto(user);
    }

    public UserDTO updateProfile(Long id, UpdateUserProfileRequest request) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setBio(request.bio());
        user.setImageUrl(request.imageUrl());

        UserEntity saved = userRepository.save(user);
        return userMapper.toDto(saved);
    }

    public UserDTO assignRole(Long id, Role role) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        user.grantRole(role);
        UserEntity saved = userRepository.save(user);
        return userMapper.toDto(saved);
    }

    public UserDTO revokeRole(Long id, Role role) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        user.revokeRole(role);
        UserEntity saved = userRepository.save(user);
        return userMapper.toDto(saved);
    }
}
