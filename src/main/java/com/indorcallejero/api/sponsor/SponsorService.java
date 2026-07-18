package com.indorcallejero.api.sponsor;

import com.indorcallejero.api.storage.StorageService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class SponsorService {

    private final SponsorRepository sponsorRepository;
    private final SponsorMapper sponsorMapper;
    private final StorageService storageService;

    public SponsorService(SponsorRepository sponsorRepository, SponsorMapper sponsorMapper, StorageService storageService) {
        this.sponsorRepository = sponsorRepository;
        this.sponsorMapper = sponsorMapper;
        this.storageService = storageService;
    }

    public Page<SponsorDTO> getSponsors(Pageable pageable) {
        return sponsorRepository.findAll(pageable).map(sponsorMapper::toDto);
    }

    public SponsorDTO getSponsorById(Long id) {
        return sponsorMapper.toDto(findOrThrow(id));
    }

    public SponsorDTO createSponsor(CreateSponsorRequest request) {
        SponsorEntity sponsor = new SponsorEntity(request.name(), request.websiteUrl());
        return sponsorMapper.toDto(sponsorRepository.save(sponsor));
    }

    public SponsorDTO updateSponsor(Long id, UpdateSponsorRequest request) {
        SponsorEntity sponsor = findOrThrow(id);
        sponsor.setName(request.name());
        sponsor.setWebsiteUrl(request.websiteUrl());
        sponsor.setPhotoUrl(request.photoUrl());
        return sponsorMapper.toDto(sponsorRepository.save(sponsor));
    }

    public SponsorDTO updatePhoto(Long id, MultipartFile file) {
        SponsorEntity sponsor = findOrThrow(id);
        String key = storageService.store(file, "sponsors");
        sponsor.setPhotoUrl("/api/files/" + key);
        return sponsorMapper.toDto(sponsorRepository.save(sponsor));
    }

    public void deleteSponsor(Long id) {
        if (!sponsorRepository.existsById(id)) {
            throw new SponsorNotFoundException(id);
        }
        sponsorRepository.deleteById(id);
    }

    private SponsorEntity findOrThrow(Long id) {
        return sponsorRepository.findById(id).orElseThrow(() -> new SponsorNotFoundException(id));
    }
}
