package com.portfolioservice.service;

import com.portfolioservice.dal.dto.CreateInvestorRequest;
import com.portfolioservice.dal.entity.Investor;
import com.portfolioservice.dal.repository.InvestorRepository;
import com.portfolioservice.enums.StatusEnum;
import com.portfolioservice.exception.DuplicateInvestorException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InvestorService {

    private final InvestorRepository investorRepository;

    public Long createInvestor(CreateInvestorRequest createInvestorRequest) {

        if (investorRepository.existsByEmail(createInvestorRequest.getEmail())) {
            throw new DuplicateInvestorException(createInvestorRequest.getEmail());
        }

        Investor investor = Investor.builder()
                .name(createInvestorRequest.getName())
                .email(createInvestorRequest.getEmail())
                .riskProfile(createInvestorRequest.getRiskProfile())
                .status(StatusEnum.A)
                .build();

        return investorRepository.save(investor).getId();
    }
}
