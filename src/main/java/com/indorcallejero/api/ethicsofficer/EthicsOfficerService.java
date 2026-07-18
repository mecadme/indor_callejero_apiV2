package com.indorcallejero.api.ethicsofficer;

import com.indorcallejero.api.storage.StorageService;
import com.indorcallejero.api.team.TeamEntity;
import com.indorcallejero.api.team.TeamNotFoundException;
import com.indorcallejero.api.team.TeamRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

// @Transactional por la misma razón que PlayerService: "team" es LAZY y
// EthicsOfficerDTO lo expone (teamId/teamName), así que el mapeo necesita
// la sesión de Hibernate todavía abierta.
@Service
@Transactional
public class EthicsOfficerService {

    private final EthicsOfficerRepository ethicsOfficerRepository;
    private final TeamRepository teamRepository;
    private final EthicsOfficerMapper ethicsOfficerMapper;
    private final StorageService storageService;

    public EthicsOfficerService(
            EthicsOfficerRepository ethicsOfficerRepository,
            TeamRepository teamRepository,
            EthicsOfficerMapper ethicsOfficerMapper,
            StorageService storageService
    ) {
        this.ethicsOfficerRepository = ethicsOfficerRepository;
        this.teamRepository = teamRepository;
        this.ethicsOfficerMapper = ethicsOfficerMapper;
        this.storageService = storageService;
    }

    @Transactional(readOnly = true)
    public Page<EthicsOfficerDTO> getEthicsOfficers(Pageable pageable) {
        return ethicsOfficerRepository.findAll(pageable).map(ethicsOfficerMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<EthicsOfficerDTO> getEthicsOfficersByTeam(Long teamId, Pageable pageable) {
        return ethicsOfficerRepository.findByTeamId(teamId, pageable).map(ethicsOfficerMapper::toDto);
    }

    @Transactional(readOnly = true)
    public EthicsOfficerDTO getEthicsOfficerById(Long id) {
        return ethicsOfficerMapper.toDto(findOrThrow(id));
    }

    public EthicsOfficerDTO createEthicsOfficer(CreateEthicsOfficerRequest request) {
        EthicsOfficerEntity officer = new EthicsOfficerEntity(request.firstName(), request.lastName());
        return ethicsOfficerMapper.toDto(ethicsOfficerRepository.save(officer));
    }

    public EthicsOfficerDTO updateEthicsOfficer(Long id, UpdateEthicsOfficerRequest request) {
        EthicsOfficerEntity officer = findOrThrow(id);
        officer.setFirstName(request.firstName());
        officer.setLastName(request.lastName());
        officer.setPhotoUrl(request.photoUrl());
        return ethicsOfficerMapper.toDto(ethicsOfficerRepository.save(officer));
    }

    public EthicsOfficerDTO updatePhoto(Long id, MultipartFile file) {
        EthicsOfficerEntity officer = findOrThrow(id);
        String key = storageService.store(file, "ethics-officers");
        officer.setPhotoUrl("/api/files/" + key);
        return ethicsOfficerMapper.toDto(ethicsOfficerRepository.save(officer));
    }

    public EthicsOfficerDTO assignTeam(Long id, AssignTeamRequest request) {
        EthicsOfficerEntity officer = findOrThrow(id);
        if (request.teamId() == null) {
            officer.setTeam(null);
        } else {
            TeamEntity team = teamRepository.findById(request.teamId())
                    .orElseThrow(() -> new TeamNotFoundException(request.teamId()));
            officer.setTeam(team);
        }
        return ethicsOfficerMapper.toDto(ethicsOfficerRepository.save(officer));
    }

    public void deleteEthicsOfficer(Long id) {
        if (!ethicsOfficerRepository.existsById(id)) {
            throw new EthicsOfficerNotFoundException(id);
        }
        ethicsOfficerRepository.deleteById(id);
    }

    private EthicsOfficerEntity findOrThrow(Long id) {
        return ethicsOfficerRepository.findById(id).orElseThrow(() -> new EthicsOfficerNotFoundException(id));
    }
}
