package fr.arthurbr02.deploymanager.service;

import fr.arthurbr02.deploymanager.entity.MfaCode;
import fr.arthurbr02.deploymanager.repository.MfaCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MfaAttemptStore {

    private static final int MAX_ATTEMPTS = 5;

    private final MfaCodeRepository mfaCodeRepository;

    /** REQUIRES_NEW : l'appelant lève ensuite une exception qui annulerait ces écritures. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void discard(UUID mfaCodeId) {
        mfaCodeRepository.deleteById(mfaCodeId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int recordFailedAttempt(UUID mfaCodeId) {
        mfaCodeRepository.incrementAttempts(mfaCodeId);
        int attempts = mfaCodeRepository.findById(mfaCodeId).map(MfaCode::getAttempts).orElse(MAX_ATTEMPTS);
        int remaining = MAX_ATTEMPTS - attempts;
        if (remaining <= 0) {
            mfaCodeRepository.deleteById(mfaCodeId);
        }
        return remaining;
    }
}
