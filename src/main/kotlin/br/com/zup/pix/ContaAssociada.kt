package br.com.zup.pix

import br.com.zup.shared.validation.ValidUUID
import java.util.*
import javax.persistence.Column
import javax.persistence.Embeddable
import javax.persistence.Embedded
import javax.validation.Valid
import javax.validation.constraints.NotNull

@Embeddable
class ContaAssociada(
    @field:NotNull @Column(nullable = false) val tipo: String,
    @field:Valid @Embedded val instituicao: Instituicao,
    @field:NotNull @Column(nullable = false) val agencia: String,
    @field:NotNull @Column(nullable = false) val numero: String,
    @field:Valid @Embedded val titular: Titular
) {

    companion object {
        const val ITAU_UNIBANCO_ISBP = "60701190"
    }

    @Embeddable
    class Instituicao(
        @field:NotNull @Column(nullable = false) val nomeInstituicao: String,
        @field:NotNull @Column(nullable = false) val ispb: String
    )

    @Embeddable
    class Titular(
        @field:NotNull @Column(nullable = false) @ValidUUID val titularId: UUID,
        @field:NotNull @Column(nullable = false) val nomeTitular: String,
        @field:NotNull @Column(nullable = false) val cpf: String
    )
}