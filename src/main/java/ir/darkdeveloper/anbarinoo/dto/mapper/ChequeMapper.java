package ir.darkdeveloper.anbarinoo.dto.mapper;

import ir.darkdeveloper.anbarinoo.config.StartupConfig;
import ir.darkdeveloper.anbarinoo.dto.ChequeDto;
import ir.darkdeveloper.anbarinoo.model.ChequeModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface ChequeMapper {

    @Mappings({
            @Mapping(target = "userId", source = "model.user.id"),
            @Mapping(target = "issuedAt", dateFormat = StartupConfig.DATE_FORMAT),
            @Mapping(target = "validTill", dateFormat = StartupConfig.DATE_FORMAT),
            @Mapping(target = "createdAt", dateFormat = StartupConfig.DATE_FORMAT),
            @Mapping(target = "updatedAt", dateFormat = StartupConfig.DATE_FORMAT)
    })
    ChequeDto chequeToDto(ChequeModel model);

}
