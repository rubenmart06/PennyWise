package cnu.ssd.pennywise.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import cnu.ssd.pennywise.entity.HouseholdEntity;
import cnu.ssd.pennywise.entity.InvitationEntity;
import cnu.ssd.pennywise.entity.MembershipEntity;
import cnu.ssd.pennywise.entity.UserEntity;
import cnu.ssd.pennywise.exception.HouseholdException;
import cnu.ssd.pennywise.model.*;
import cnu.ssd.pennywise.repository.HouseholdRepository;
import cnu.ssd.pennywise.repository.InvitationRepository;
import cnu.ssd.pennywise.repository.MembershipRepository;
import cnu.ssd.pennywise.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for household creation, listing and membership management.
 */
@Service
public class HouseholdService {

    private final HouseholdRepository householdRepository;
    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final InvitationRepository invitationRepository;
    private final DebtSettlementService debtSettlementService;

    public HouseholdService(HouseholdRepository householdRepository, MembershipRepository membershipRepository,
                            UserRepository userRepository, InvitationRepository invitationRepository,
                            DebtSettlementService debtSettlementService) {
        this.householdRepository = householdRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.invitationRepository = invitationRepository;
        this.debtSettlementService = debtSettlementService;
    }

    /**
     * Creates a new household and assigns {@code creatorUserId} as its Host.
     *
     * @throws HouseholdException if the name/currency is missing or the user does not exist
     */
    @Transactional
    public Household createHousehold(Long creatorUserId, String name, HouseholdKind kind, String primaryCurrency) {
        String normalizedName = name == null ? "" : name.trim();
        String normalizedCurrency = primaryCurrency == null ? "" : primaryCurrency.trim().toUpperCase();

        if (normalizedName.isEmpty()) {
            throw new HouseholdException("Household name is required.");
        }
        if (normalizedCurrency.length() != 3) {
            throw new HouseholdException("Primary currency must be a 3-letter code (e.g. EUR).");
        }
        if (kind == null) {
            throw new HouseholdException("Household kind is required.");
        }

        UserEntity creator = userRepository.findById(creatorUserId)
                .orElseThrow(() -> new HouseholdException("Unknown user."));

        HouseholdEntity newHouseholdEntity = new HouseholdEntity();
        newHouseholdEntity.setName(normalizedName);
        newHouseholdEntity.setKind(kind);
        newHouseholdEntity.setPrimaryCurrency(normalizedCurrency);
        HouseholdEntity household = householdRepository.save(newHouseholdEntity);

        MembershipEntity newHostMembership = new MembershipEntity();
        newHostMembership.setUser(creator);
        newHostMembership.setHousehold(household);
        newHostMembership.setRole(MemberRole.HOST);
        MembershipEntity hostMembership = membershipRepository.save(newHostMembership);

        return toModel(household, List.of(hostMembership));
    }

    /** Every household the given user belongs to, with its member list populated. */
    @Transactional(readOnly = true)
    public List<Household> getHouseholdsForUser(Long userId) {
        List<HouseholdEntity> households = householdRepository.findAllByMemberUserId(userId);
        List<Household> result = new ArrayList<>(households.size());
        for (HouseholdEntity household : households) {
            List<MembershipEntity> memberships = membershipRepository.findByHousehold(household);
            result.add(toModel(household, memberships));
        }
        return result;
    }

    /**
     * Adds an already-registered user to a household as a Member. The user
     * is added immediately; the next time they log in the household appears in their hub.
     *
     * @throws HouseholdException if the household/username is unknown, the requester is not the Host,
     *                            or the user is already a member
     */
    @Transactional
    public Member addMember(Long householdId, Long requesterUserId, String username) {
        HouseholdEntity household = householdRepository.findById(householdId)
                .orElseThrow(() -> new HouseholdException("Unknown household."));
        requireHost(household, requesterUserId);
        if (username == null || username.isBlank()) {
            throw new HouseholdException("Enter a username.");
        }
        UserEntity user = userRepository.findByUsername(username == null ? "" : username.trim())
                .orElseThrow(() -> new HouseholdException("No registered user with that username."));

        if (membershipRepository.existsByHouseholdAndUser(household, user)) {
            throw new HouseholdException("That user is already a member of this household.");
        }

        MembershipEntity newMembershipEntity = new MembershipEntity();
        newMembershipEntity.setUser(user);
        newMembershipEntity.setHousehold(household);
        newMembershipEntity.setRole(MemberRole.MEMBER);
        MembershipEntity membership = membershipRepository.save(newMembershipEntity);
        return toMemberModel(membership);
    }

    /**
     * Removes a member from a household, blocking it if they have any pending
     * debts, settle those first.
     *
     * @throws HouseholdException if the household/user is unknown, the requester is not the Host,
     *                            the user is not a member (or is the Host), or they have a pending
     *                            debt in either direction
     */
    @Transactional
    public void removeMember(Long householdId, Long requesterUserId, Long userId) {
        HouseholdEntity household = householdRepository.findById(householdId)
                .orElseThrow(() -> new HouseholdException("Unknown household."));
        requireHost(household, requesterUserId);
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new HouseholdException("Unknown user."));
        MembershipEntity membership = membershipRepository.findByHouseholdAndUser(household, user)
                .orElseThrow(() -> new HouseholdException("That user is not a member of this household."));

        if (membership.getRole() == MemberRole.HOST) {
            throw new HouseholdException("The household's Host can't be removed.");
        }

        boolean hasPendingDebt = debtSettlementService.getOptimizedDebts(householdId).stream()
                .anyMatch(debt -> debt.getFromUserId().equals(userId) || debt.getToUserId().equals(userId));
        if (hasPendingDebt) {
            throw new HouseholdException("This member has pending debts — settle them first.");
        }

        membershipRepository.delete(membership);
    }

    /** One household with its current member list (the open household's cached copy goes stale after membership changes). */
    @Transactional(readOnly = true)
    public Household getHousehold(Long householdId) {
        HouseholdEntity household = householdRepository.findById(householdId)
                .orElseThrow(() -> new HouseholdException("Unknown household."));
        return toModel(household, membershipRepository.findByHousehold(household));
    }

    private void requireHost(HouseholdEntity household, Long requesterUserId) {
        boolean isHost = membershipRepository.findByHousehold(household).stream()
                .anyMatch(m -> m.getRole() == MemberRole.HOST && m.getUser().getId().equals(requesterUserId));
        if (!isHost) {
            throw new HouseholdException("Only the household's Host can add or remove members.");
        }
    }

    /**
     * Returns the household's single pending invitation, generating a new one
     * if none exists yet. The "link" is a display string built from a random
     * token; there's no real web server to resolve it to, so acceptance
     * happens through {@link #acceptInvitation} directly.
     */
    @Transactional
    public Invitation getOrCreateInvitation(Long householdId) {
        if (!householdRepository.existsById(householdId)) {
            throw new HouseholdException("Unknown household.");
        }
        InvitationEntity invitation = invitationRepository
                .findByHousehold_IdAndStatus(householdId, InvitationStatus.PENDING)
                .orElseGet(() -> {
                    HouseholdEntity household = householdRepository.getReferenceById(householdId);
                    String token = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                    InvitationEntity newInvitationEntity = new InvitationEntity();
                    newInvitationEntity.setHousehold(household);
                    newInvitationEntity.setToken(token);
                    newInvitationEntity.setStatus(InvitationStatus.PENDING);
                    return invitationRepository.save(newInvitationEntity);
                });
        return toInvitationModel(invitation);
    }

    /**
     * Accepts an invitation: adds the given user as a Member and marks the
     * invitation accepted.
     *
     * @throws HouseholdException if the token is unknown, already accepted, or the user is already a member
     */
    @Transactional
    public Member acceptInvitation(String token, Long acceptingUserId) {
        InvitationEntity invitation = invitationRepository.findByToken(token)
                .orElseThrow(() -> new HouseholdException("Invalid invitation link."));
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new HouseholdException("This invitation has already been used.");
        }
        HouseholdEntity household = invitation.getHousehold();
        UserEntity user = userRepository.findById(acceptingUserId)
                .orElseThrow(() -> new HouseholdException("Unknown user."));
        if (membershipRepository.existsByHouseholdAndUser(household, user)) {
            throw new HouseholdException("You're already a member of this household.");
        }

        MembershipEntity newMembership = new MembershipEntity();
        newMembership.setUser(user);
        newMembership.setHousehold(household);
        newMembership.setRole(MemberRole.MEMBER);
        MembershipEntity membership = membershipRepository.save(newMembership);
        invitation.setStatus(InvitationStatus.ACCEPTED);
        return toMemberModel(membership);
    }

    private Invitation toInvitationModel(InvitationEntity entity) {
        String link = "pennywise.local/join/" + entity.getToken();
        return new Invitation(entity.getHousehold().getId(), entity.getToken(), link, entity.getStatus());
    }

    private Household toModel(HouseholdEntity entity, List<MembershipEntity> memberships) {
        List<Member> members = memberships.stream().map(this::toMemberModel).collect(Collectors.toList());
        return new Household(entity.getId(), entity.getName(), entity.getKind(), entity.getPrimaryCurrency(), members);
    }

    private Member toMemberModel(MembershipEntity membership) {
        UserEntity user = membership.getUser();
        return new Member(membership.getId(), new User(user.getId(), user.getUsername(), user.getEmail()), membership.getRole());
    }
}
