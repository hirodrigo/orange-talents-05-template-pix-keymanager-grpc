package br.com.zup.pix.consulta

import br.com.zup.ConsultaChavePixResponse
import br.com.zup.pix.ChavePix
import br.com.zup.pix.ContaAssociada
import br.com.zup.pix.TipoChave
import br.com.zup.pix.TipoConta
import com.google.protobuf.Timestamp
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

data class ChavePixInfo(
    val pixId: UUID? = null,
    val clienteId: UUID? = null,
    val tipoChave: TipoChave,
    val chave: String,
    val tipoConta: TipoConta,
    val conta: ContaChavePixInfo,
    val registradaEm: LocalDateTime = LocalDateTime.now()
) {

    companion object {
        fun of(chavePix: ChavePix): ChavePixInfo {
            return ChavePixInfo(
                pixId = chavePix.id,
                clienteId = chavePix.clienteId,
                tipoChave = chavePix.tipoChave,
                chave = chavePix.chave,
                tipoConta = chavePix.tipoConta,
                conta = ContaChavePixInfo.of(chavePix.conta),
                registradaEm = chavePix.criadaEm
            )
        }
    }

    fun toConsultaChavePixResponse(): ConsultaChavePixResponse {
        return ConsultaChavePixResponse.newBuilder()
            .setClienteId(this.clienteId.toString() ?: "")
            .setPixId(this.pixId.toString() ?: "")
            .setChave(
                ConsultaChavePixResponse.ChavePix.newBuilder()
                    .setTipo(br.com.zup.TipoChave.valueOf(this.tipoChave.name))
                    .setChave(this.chave)
                    .setConta(
                        ConsultaChavePixResponse.ChavePix.ContaInfo.newBuilder()
                            .setTipo(br.com.zup.TipoConta.valueOf(this.conta.tipo))
                            .setInstituicao(this.conta.nomeInstituicao)
                            .setNomeDoTitular(this.conta.nomeTitular)
                            .setCpfDoTitular(this.conta.cpfTitular)
                            .setAgencia(this.conta.agencia)
                            .setNumeroDaConta(this.conta.numero)
                            .build()
                    )
                    .setCriadaEm(
                        this.registradaEm.let {
                            val createdAt = it.atZone(ZoneId.of("UTC")).toInstant()
                            Timestamp.newBuilder()
                                .setSeconds(createdAt.epochSecond)
                                .setNanos(createdAt.nano)
                                .build()
                        }
                    )
                    .build()
            )
            .build()
    }

    data class ContaChavePixInfo(
        val tipo: String,
        val nomeInstituicao: String,
        val nomeTitular: String,
        val cpfTitular: String,
        val agencia: String,
        val numero: String,
    ) {
        companion object {
            fun of(contaAssociada: ContaAssociada): ContaChavePixInfo {
                return ContaChavePixInfo(
                    tipo = contaAssociada.tipo,
                    nomeInstituicao = contaAssociada.instituicao.nomeInstituicao,
                    nomeTitular = contaAssociada.titular.nomeTitular,
                    cpfTitular = contaAssociada.titular.cpf,
                    agencia = contaAssociada.agencia,
                    numero = contaAssociada.numero
                )
            }
        }
    }

}
