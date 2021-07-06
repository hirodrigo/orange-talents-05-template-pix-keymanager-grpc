package br.com.zup.pix.remove

import br.com.zup.pix.ChavePixRepository
import br.com.zup.pix.registra.ChavePix
import io.micronaut.validation.Validated
import org.slf4j.LoggerFactory
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import javax.transaction.Transactional
import javax.validation.Valid

@Singleton
@Validated
class RemoveChave(
    @Inject val chavePixRepository: ChavePixRepository
) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @Transactional
    fun remove(@Valid dto: RemoveChavePixDTO): ChavePix {
        val possivelChavePix = chavePixRepository.findByIdAndClienteId(
            id = UUID.fromString(dto.pixId),
            clientId = UUID.fromString(dto.clienteId)
        )

        if (possivelChavePix.isEmpty) {
            throw ChavePixNaoEncontradaException("Chave Pix não encontrada ou não pertence ao cliente.")
        }
        val chavePix = possivelChavePix.get()
        chavePixRepository.delete(chavePix)
        logger.info("Chave Pix (${chavePix.id}) removida.")

        return chavePix
    }
}
