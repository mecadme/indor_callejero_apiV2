package com.indorcallejero.api.round;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class RoundService {

    private final RoundRepository roundRepository;
    private final RoundMapper roundMapper;

    public RoundService(RoundRepository roundRepository, RoundMapper roundMapper) {
        this.roundRepository = roundRepository;
        this.roundMapper = roundMapper;
    }

    public Page<RoundDTO> getRounds(Pageable pageable) {
        return roundRepository.findAllByOrderByNumberAsc(pageable).map(roundMapper::toDto);
    }

    public RoundDTO getRoundById(Long id) {
        return roundMapper.toDto(findOrThrow(id));
    }

    public RoundDTO createRound(CreateRoundRequest request) {
        RoundEntity round = new RoundEntity(request.name(), request.number());
        return roundMapper.toDto(roundRepository.save(round));
    }

    private RoundEntity findOrThrow(Long id) {
        return roundRepository.findById(id).orElseThrow(() -> new RoundNotFoundException(id));
    }
}
