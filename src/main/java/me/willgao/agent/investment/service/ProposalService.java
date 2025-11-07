package me.willgao.agent.investment.service;

import me.willgao.agent.investment.type.PortfolioResponse;
import me.willgao.agent.investment.type.Proposal;
import me.willgao.agent.investment.type.Rating;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class ProposalService {


    private Proposal currentProposal = null;
    private Rating currentRating = null;


    @EventListener
    public void onProposal(Proposal proposal) {
        currentProposal = proposal;
    }

    @EventListener
    public void onRatingRequest(Rating rating) {
        currentRating = rating;
    }

    public PortfolioResponse getCurrentPortfolioResponse() {
        return new PortfolioResponse(currentProposal, currentRating);
    }

    public void clear() {
        currentProposal = null;
        currentRating = null;
    }
}
