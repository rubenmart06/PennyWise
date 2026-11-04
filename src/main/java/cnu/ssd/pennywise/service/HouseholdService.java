package cnu.ssd.pennywise.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import cnu.ssd.pennywise.entity.HouseholdEntity;
import cnu.ssd.pennywise.entity.MembershipEntity;
import cnu.ssd.pennywise.entity.UserEntity;
import cnu.ssd.pennywise.exception.HouseholdException;
import cnu.ssd.pennywise.model.*;
import cnu.ssd.pennywise.repository.HouseholdRepository;
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

    public HouseholdService(HouseholdRepository householdRepository, MembershipRepository membershipRepository,
                            UserRepository userRepository) {
        this.householdRepository = householdRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
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
     * Adds an already-registered user to a household as a Member.
     *
     * @throws HouseholdException if the household/username is unknown, or the user is already a member
     */
    @Transactional
    public Member addMember(Long householdId, String username) {
        HouseholdEntity household = householdRepository.findById(householdId)
                .orElseThrow(() -> new HouseholdException("Unknown household."));
        UserEntity user = userRepository.findByUsername(username == null ? "" : username.trim())
                .orElseThrow(() -> new HouseholdException("No registered user with that username."));

        if (membershipRepository.existsByHouseholdAndUser(household, user)) {
            throw new HouseholdException("That user is already a member of this household.");
        }

        MembershipEntity newMembership = new MembershipEntity();
        newMembership.setUser(user);
        newMembership.setHousehold(household);
        newMembership.setRole(MemberRole.MEMBER);
        MembershipEntity membership = membershipRepository.save(newMembership);
        return toMemberModel(membership);
    }

    /**
     * Removes a member from a household.
     *
     * @throws HouseholdException if the household/user is unknown, or the user is not a member
     */
    @Transactional
    public void removeMember(Long householdId, Long userId) {
        HouseholdEntity household = householdRepository.findById(householdId)
                .orElseThrow(() -> new HouseholdException("Unknown household."));
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new HouseholdException("Unknown user."));
        MembershipEntity membership = membershipRepository.findByHouseholdAndUser(household, user)
                .orElseThrow(() -> new HouseholdException("That user is not a member of this household."));

        membershipRepository.delete(membership);
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
