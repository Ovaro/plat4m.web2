package ovaro.plat4m.service;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ovaro.plat4m.domain.Source;
import ovaro.plat4m.domain.User;
import ovaro.plat4m.repository.SourceRepository;

@Service
public class SourceService {

    private final Logger log = LoggerFactory.getLogger(SourceService.class);

    private SourceRepository sourceRepository;

    public SourceService(SourceRepository sourceRepository) {
        this.sourceRepository = sourceRepository;
    }

    public List<Source> listAllForUser(User user) {
        return this.sourceRepository.findByUserGuid(user.getGuid().toString());
    }
}
