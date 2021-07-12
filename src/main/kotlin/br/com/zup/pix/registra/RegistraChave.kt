package br.com.zup.pix.registra

import br.com.zup.integracao.bcb.BCBPixClient
import br.com.zup.integracao.bcb.CreatePixKeyRequest
import br.com.zup.integracao.itau.ItauContasClient
import br.com.zup.pix.ChavePix
import br.com.zup.pix.ChavePixRepository
import io.micronaut.http.HttpStatus
import io.micronaut.validation.Validated
import org.slf4j.LoggerFactory
import javax.inject.Inject
import javax.inject.Singleton
import javax.transaction.Transactional
import javax.validation.Valid

@Validated
@Singleton
class RegistraChave(
    @Inject val chavePixRepository: ChavePixRepository,
    @Inject val itauContasClient: ItauContasClient,
    @Inject val bcbPixClient: BCBPixClient
) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @Transactional
    fun registra(@Valid novaChave: NovaChavePix): ChavePix {

        if (chavePixRepository.existsByChave(novaChave.chave)) {
            throw ChavePixExistenteException("Chave Pix ${novaChave.chave} já existente.")
        }

        val itauResponse = itauContasClient.buscaContaPorTipo(novaChave.clienteId, novaChave.tipoConta!!.name)
        val conta = itauResponse.body()?.toModel() ?: throw IllegalStateException("Cliente não encontrado no Itaú.")
        val chave = novaChave.toModel(conta)

        val bcbResponse = bcbPixClient.criaNovaChave(CreatePixKeyRequest.of(chave))

        if (bcbResponse.status != HttpStatus.CREATED) {
            throw java.lang.IllegalStateException("Falha ao cadastrar chave no Banco Central")
        }

        chave.atualizaChave(bcbResponse.body()!!.key)

        chavePixRepository.save(chave)
        logger.info("Chave salva com sucesso: ${chave.id}")

        return chave
    }

}