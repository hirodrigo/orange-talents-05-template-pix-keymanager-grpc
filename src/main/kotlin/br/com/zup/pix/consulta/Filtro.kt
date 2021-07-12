package br.com.zup.pix.consulta

import br.com.zup.integracao.bcb.BCBPixClient
import br.com.zup.pix.ChavePixRepository
import br.com.zup.pix.remove.ChavePixNaoEncontradaException
import br.com.zup.shared.validation.ValidUUID
import io.micronaut.core.annotation.Introspected
import io.micronaut.http.HttpStatus
import org.slf4j.LoggerFactory
import java.util.*
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Size

@Introspected
sealed class Filtro {

    abstract fun filtra(repository: ChavePixRepository, bcbPixClient: BCBPixClient): ChavePixInfo

    @Introspected
    data class PorPixId(
        @field:NotBlank @field:ValidUUID val clientId: String,
        @field:NotBlank @field:ValidUUID val pixId: String
    ) : Filtro() {

        fun clienteIdAsUuid() = UUID.fromString(clientId)
        fun pixIdAsUuid() = UUID.fromString(pixId)

        override fun filtra(repository: ChavePixRepository, bcbPixClient: BCBPixClient): ChavePixInfo {
            return repository.findById(pixIdAsUuid())
                .filter() { it.pertenceAo(clienteIdAsUuid()) }
                .map(ChavePixInfo::of)
                .orElseThrow { ChavePixNaoEncontradaException("Chave Pix não encontrada") }
        }
    }

    @Introspected
    data class PorChave(@field:NotBlank @Size(max = 77) val chave: String) : Filtro() {

        private val logger = LoggerFactory.getLogger(this::class.java)

        override fun filtra(repository: ChavePixRepository, bcbPixClient: BCBPixClient): ChavePixInfo {
            return repository.findByChave(chave)
                .map(ChavePixInfo::of)
                .orElseGet {
                    logger.info("Consultando chave Pix ${chave} no Banco Central")

                    val response = bcbPixClient.buscaChave(chave)
                    when (response.status) {
                        HttpStatus.OK -> response.body()?.paraChavePixInfo()
                        else -> throw ChavePixNaoEncontradaException("Chave Pix não encontrada")
                    }
                }
        }
    }

    @Introspected
    class Invalido() : Filtro() {
        override fun filtra(repository: ChavePixRepository, bcbPixClient: BCBPixClient): ChavePixInfo {
            throw IllegalArgumentException("Chave Pix inválida ou não informada")
        }
    }

}
