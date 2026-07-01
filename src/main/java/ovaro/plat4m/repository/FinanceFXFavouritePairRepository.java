package ovaro.plat4m.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ovaro.plat4m.domain.FinanceFXFavouritePair;

@Repository
public interface FinanceFXFavouritePairRepository extends JpaRepository<FinanceFXFavouritePair, UUID> {
    List<FinanceFXFavouritePair> findAllByUserGuid(String userGuid);

    Optional<FinanceFXFavouritePair> findByUserGuidAndFromIsoCodeAndToIsoCode(String userGuid, String fromIsoCode, String toIsoCode);
}
