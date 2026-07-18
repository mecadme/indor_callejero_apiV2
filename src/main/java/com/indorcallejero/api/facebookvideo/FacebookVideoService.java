package com.indorcallejero.api.facebookvideo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class FacebookVideoService {

    private final FacebookVideoRepository facebookVideoRepository;
    private final FacebookVideoMapper facebookVideoMapper;

    public FacebookVideoService(FacebookVideoRepository facebookVideoRepository, FacebookVideoMapper facebookVideoMapper) {
        this.facebookVideoRepository = facebookVideoRepository;
        this.facebookVideoMapper = facebookVideoMapper;
    }

    public Page<FacebookVideoDTO> getFacebookVideos(Pageable pageable) {
        return facebookVideoRepository.findAll(pageable).map(facebookVideoMapper::toDto);
    }

    public FacebookVideoDTO getFacebookVideoById(Long id) {
        return facebookVideoMapper.toDto(findOrThrow(id));
    }

    public FacebookVideoDTO createFacebookVideo(CreateFacebookVideoRequest request) {
        FacebookVideoEntity video = new FacebookVideoEntity(request.title(), request.videoUrl());
        return facebookVideoMapper.toDto(facebookVideoRepository.save(video));
    }

    public FacebookVideoDTO updateFacebookVideo(Long id, UpdateFacebookVideoRequest request) {
        FacebookVideoEntity video = findOrThrow(id);
        video.setTitle(request.title());
        video.setVideoUrl(request.videoUrl());
        return facebookVideoMapper.toDto(facebookVideoRepository.save(video));
    }

    public void deleteFacebookVideo(Long id) {
        if (!facebookVideoRepository.existsById(id)) {
            throw new FacebookVideoNotFoundException(id);
        }
        facebookVideoRepository.deleteById(id);
    }

    private FacebookVideoEntity findOrThrow(Long id) {
        return facebookVideoRepository.findById(id).orElseThrow(() -> new FacebookVideoNotFoundException(id));
    }
}
