package com.indorcallejero.api.team;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMapper teamMapper;

    public TeamService(TeamRepository teamRepository, TeamMapper teamMapper) {
        this.teamRepository = teamRepository;
        this.teamMapper = teamMapper;
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
