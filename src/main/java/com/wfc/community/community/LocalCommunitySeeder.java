package com.wfc.community.community;

import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
class LocalCommunitySeeder implements CommandLineRunner {

    private final CommunityRepository communityRepository;

    LocalCommunitySeeder(CommunityRepository communityRepository) {
        this.communityRepository = communityRepository;
    }

    @Override
    public void run(String... args) {
        if (communityRepository.count() != 0) {
            return;
        }
        communityRepository.saveAll(List.of(
                new Community(
                        "Arsenal Women",
                        "arsenal-women",
                        "For the Arsenal Women's team and everyone cheering them on."),
                new Community(
                        "Chelsea Women",
                        "chelsea-women",
                        "The home of Chelsea FC Women supporters."),
                new Community(
                        "FC Barcelona Femení",
                        "barcelona-femeni",
                        "Blaugrana hearts, one team. Visca el Barça!"),
                new Community(
                        "USWNT",
                        "uswnt",
                        "One nation. One team. All for the stars and stripes.")));
    }
}
