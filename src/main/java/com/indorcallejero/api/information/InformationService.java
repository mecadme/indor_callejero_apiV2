package com.indorcallejero.api.information;

import com.indorcallejero.api.storage.StorageService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class InformationService {

    private final InformationRepository informationRepository;
    private final InformationMapper informationMapper;
    private final StorageService storageService;

    public InformationService(InformationRepository informationRepository, InformationMapper informationMapper, StorageService storageService) {
        this.informationRepository = informationRepository;
        this.informationMapper = informationMapper;
        this.storageService = storageService;
    }

    public Page<InformationDTO> getInformation(Pageable pageable) {
        return informationRepository.findAll(pageable).map(informationMapper::toDto);
    }

    public InformationDTO getInformationById(Long id) {
        return informationMapper.toDto(findOrThrow(id));
    }

    public InformationDTO createInformation(CreateInformationRequest request) {
        InformationEntity information = new InformationEntity(request.title(), request.content());
        return informationMapper.toDto(informationRepository.save(information));
    }

    public InformationDTO updateInformation(Long id, UpdateInformationRequest request) {
        InformationEntity information = findOrThrow(id);
        information.setTitle(request.title());
        information.setContent(request.content());
        information.setPhotoUrl(request.photoUrl());
        return informationMapper.toDto(informationRepository.save(information));
    }

    public InformationDTO updatePhoto(Long id, MultipartFile file) {
        InformationEntity information = findOrThrow(id);
        String key = storageService.store(file, "information");
        information.setPhotoUrl("/api/files/" + key);
        return informationMapper.toDto(informationRepository.save(information));
    }

    public void deleteInformation(Long id) {
        if (!informationRepository.existsById(id)) {
            throw new InformationNotFoundException(id);
        }
        informationRepository.deleteById(id);
    }

    private InformationEntity findOrThrow(Long id) {
        return informationRepository.findById(id).orElseThrow(() -> new InformationNotFoundException(id));
    }
}
