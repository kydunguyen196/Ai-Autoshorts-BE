package com.autoshorts.ai.service;

import com.autoshorts.ai.dto.ChannelCreateRequest;
import com.autoshorts.ai.dto.ChannelResponse;
import com.autoshorts.ai.entity.Channel;
import com.autoshorts.ai.exception.BadRequestException;
import com.autoshorts.ai.exception.ResourceNotFoundException;
import com.autoshorts.ai.repository.ChannelRepository;
import com.autoshorts.ai.util.ChannelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

@Service
public class ChannelService {

    private static final Logger log = LoggerFactory.getLogger(ChannelService.class);

    private final ChannelRepository channelRepository;

    public ChannelService(ChannelRepository channelRepository) {
        this.channelRepository = channelRepository;
    }

    @Transactional(readOnly = true)
    public List<ChannelResponse> listChannels(UUID userId) {
        return channelRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
            .stream()
            .map(ChannelMapper::toResponse)
            .toList();
    }

    @Transactional
    public ChannelResponse createChannel(UUID userId, ChannelCreateRequest request) {
        String normalizedName = request.getName().trim();
        if (channelRepository.existsByUserIdAndNameIgnoreCase(userId, normalizedName)) {
            throw new BadRequestException("Channel already exists with this name");
        }
        Channel channel = new Channel();
        channel.setUserId(userId);
        channel.setName(normalizedName);
        channel.setDescription(trimToNull(request.getDescription()));
        channel.setIsDefault(false);
        Channel saved;
        try {
            saved = channelRepository.save(channel);
        } catch (DataIntegrityViolationException ex) {
            throw new BadRequestException("Channel already exists with this name");
        }
        log.info("event=channel_created userId={} channelId={} name={}", userId, saved.getId(), saved.getName());
        return ChannelMapper.toResponse(saved);
    }

    @Transactional
    public Channel ensureDefaultChannel(UUID userId) {
        return channelRepository.findFirstByUserIdAndIsDefaultTrue(userId)
            .orElseGet(() -> {
                Channel channel = new Channel();
                channel.setUserId(userId);
                channel.setName("Default");
                channel.setDescription("Default channel");
                channel.setIsDefault(true);
                try {
                    Channel saved = channelRepository.save(channel);
                    log.info("event=default_channel_created userId={} channelId={}", userId, saved.getId());
                    return saved;
                } catch (DataIntegrityViolationException ex) {
                    return channelRepository.findFirstByUserIdAndIsDefaultTrue(userId)
                        .orElseThrow(() -> ex);
                }
            });
    }

    @Transactional(readOnly = true)
    public ChannelResponse getDefaultChannelResponse(UUID userId) {
        return ChannelMapper.toResponse(ensureDefaultChannel(userId));
    }

    @Transactional(readOnly = true)
    public UUID resolveOwnedChannelIdOrDefault(UUID userId, UUID requestedChannelId) {
        if (requestedChannelId == null) {
            return ensureDefaultChannel(userId).getId();
        }
        Channel channel = channelRepository.findByIdAndUserId(requestedChannelId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Channel not found: " + requestedChannelId));
        return channel.getId();
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
