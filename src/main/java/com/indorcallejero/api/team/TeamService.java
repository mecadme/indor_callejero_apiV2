package com.indorcallejero.api.team;

import com.indorcallejero.api.storage.StorageService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMapper teamMapper;
    private final StorageService storageService;

    public TeamService(TeamRepository teamRepository, TeamMapper teamMapper, StorageService storageService) {
        this.teamRepository = teamRepository;
        this.teamMapper = teamMapper;
        this.storageService = storageService;
    }

    public Page<TeamDTO> getTeams(Pageable pageable) {
        return teamRepository.findAll(pageable).map(teamMapper::toDto);
    }

    public TeamDTO getTeamById(Long id) {
        return teamMapper.toDto(findOrThrow(id));
    }

    public TeamDTO createTeam(CreateTeamRequest request) {
        TeamEntity team = new TeamEntity(request.name(), request.color(), request.neighborhood(), request.group());
        return teamMapper.toDto(teamRepository.save(team));
    }

    public TeamDTO updateTeam(Long id, UpdateTeamRequest request) {
        TeamEntity team = findOrThrow(id);
        team.setName(request.name());
        team.setColor(request.color());
        team.setNeighborhood(request.neighborhood());
        team.setLogoUrl(request.logoUrl());
        team.setGroup(request.group());
        return teamMapper.toDto(teamRepository.save(team));
    }

    public TeamDTO updateLogo(Long id, MultipartFile file) {
        TeamEntity team = findOrThrow(id);
        String key = storageService.store(file, "teams");
        team.setLogoUrl("/api/files/" + key);
        return teamMapper.toDto(teamRepository.save(team));
    }

    public void deleteTeam(Long id) {
        if (!teamRepository.existsById(id)) {
            throw new TeamNotFoundException(id);
        }
        teamRepository.deleteById(id);
    }

    private TeamEntity findOrThrow(Long id) {
        return teamRepository.findById(id).orElseThrow(() -> new TeamNotFoundException(id));
    }
}
