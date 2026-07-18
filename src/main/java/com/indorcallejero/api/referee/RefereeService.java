package com.indorcallejero.api.referee;

import com.indorcallejero.api.storage.StorageService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class RefereeService {

    private final RefereeRepository refereeRepository;
    private final RefereeMapper refereeMapper;
    private final StorageService storageService;

    public RefereeService(RefereeRepository refereeRepository, RefereeMapper refereeMapper, StorageService storageService) {
        this.refereeRepository = refereeRepository;
        this.refereeMapper = refereeMapper;
        this.storageService = storageService;
    }

    public Page<RefereeDTO> getReferees(Pageable pageable) {
        return refereeRepository.findAll(pageable).map(refereeMapper::toDto);
    }

    public RefereeDTO getRefereeById(Long id) {
        return refereeMapper.toDto(findOrThrow(id));
    }

    public RefereeDTO createReferee(CreateRefereeRequest request) {
        RefereeEntity referee = new RefereeEntity(request.firstName(), request.lastName(), request.licenseNumber());
        return refereeMapper.toDto(refereeRepository.save(referee));
    }

    public RefereeDTO updateReferee(Long id, UpdateRefereeRequest request) {
        RefereeEntity referee = findOrThrow(id);
        referee.setFirstName(request.firstName());
        referee.setLastName(request.lastName());
        referee.setLicenseNumber(request.licenseNumber());
        referee.setPhotoUrl(request.photoUrl());
        return refereeMapper.toDto(refereeRepository.save(referee));
    }

    public RefereeDTO updatePhoto(Long id, MultipartFile file) {
        RefereeEntity referee = findOrThrow(id);
        String key = storageService.store(file, "referees");
        referee.setPhotoUrl("/api/files/" + key);
        return refereeMapper.toDto(refereeRepository.save(referee));
    }

    public void deleteReferee(Long id) {
        if (!refereeRepository.existsById(id)) {
            throw new RefereeNotFoundException(id);
        }
        refereeRepository.deleteById(id);
    }

    private RefereeEntity findOrThrow(Long id) {
        return refereeRepository.findById(id).orElseThrow(() -> new RefereeNotFoundException(id));
    }
}
