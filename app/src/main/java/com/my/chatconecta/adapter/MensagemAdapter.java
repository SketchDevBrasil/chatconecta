package com.my.chatconecta.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.my.chatconecta.R;
import com.my.chatconecta.model.Mensagem;
import com.my.chatconecta.util.Avatar;
import com.my.chatconecta.util.Tempo;

import java.util.ArrayList;
import java.util.List;

/**
 * Bolhas de mensagem usadas no chat publico e no privado. Mensagens seguidas da mesma
 * pessoa (ate 5 min de intervalo) formam um grupo: ficam mais proximas, com cantos
 * internos menos arredondados, e no chat publico o avatar aparece so na ultima.
 */
public class MensagemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface Listener {
        void onAutorClick(String uid, String nome);
    }

    private static final int TIPO_DATA = 0;
    private static final int TIPO_ENVIADA = 1;
    private static final int TIPO_RECEBIDA = 2;
    private static final long INTERVALO_GRUPO_MS = 5 * 60 * 1000L;

    /** Cada item e um Long (separador de dia) ou uma Mensagem. */
    private final List<Object> itens = new ArrayList<>();
    private final String meuUid;
    private final boolean publico;
    @Nullable
    private final Listener listener;
    private long lidoPeloContatoAte;

    public MensagemAdapter(String meuUid, boolean publico, @Nullable Listener listener) {
        this.meuUid = meuUid;
        this.publico = publico;
        this.listener = listener;
    }

    /** Adiciona no fim ou, se a mensagem ja existe (mesmo id), atualiza no lugar. */
    public void adicionar(Mensagem mensagem) {
        int existente = posicaoDe(mensagem.getId());
        if (existente >= 0) {
            itens.set(existente, mensagem);
            notifyItemChanged(existente);
            return;
        }
        Mensagem anterior = ultimaMensagem();
        if (anterior == null || !Tempo.mesmoDia(anterior.getEnviadoEm(), mensagem.getEnviadoEm())) {
            itens.add(mensagem.getEnviadoEm());
            notifyItemInserted(itens.size() - 1);
        } else {
            // A anterior pode deixar de ser a ultima do grupo (muda canto e avatar).
            notifyItemChanged(itens.size() - 1);
        }
        itens.add(mensagem);
        notifyItemInserted(itens.size() - 1);
    }

    /** Mensagens minhas enviadas ate este horario aparecem com o check duplo. */
    public void setLidoPeloContatoAte(long ms) {
        if (ms == lidoPeloContatoAte) {
            return;
        }
        lidoPeloContatoAte = ms;
        notifyItemRangeChanged(0, itens.size());
    }

    public boolean temMensagens() {
        return ultimaMensagem() != null;
    }

    @Override
    public int getItemCount() {
        return itens.size();
    }

    @Override
    public int getItemViewType(int position) {
        Object item = itens.get(position);
        if (!(item instanceof Mensagem)) {
            return TIPO_DATA;
        }
        return meuUid.equals(((Mensagem) item).getUid()) ? TIPO_ENVIADA : TIPO_RECEBIDA;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TIPO_DATA) {
            return new DataHolder(inflater.inflate(R.layout.item_data, parent, false));
        }
        if (viewType == TIPO_ENVIADA) {
            return new EnviadaHolder(inflater.inflate(R.layout.item_mensagem_enviada, parent, false));
        }
        return new RecebidaHolder(inflater.inflate(R.layout.item_mensagem_recebida, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Context context = holder.itemView.getContext();
        Object item = itens.get(position);
        if (holder instanceof DataHolder) {
            ((DataHolder) holder).txtData.setText(Tempo.dia(context, (Long) item));
            return;
        }
        Mensagem m = (Mensagem) item;
        boolean primeira = position == 0 || !mesmoGrupo(itens.get(position - 1), m);
        boolean ultima = position == itens.size() - 1 || !mesmoGrupo(m, itens.get(position + 1));
        int espacoTopo = dp(context, primeira ? 8 : 2);

        if (holder instanceof EnviadaHolder) {
            EnviadaHolder h = (EnviadaHolder) holder;
            h.linha.setPadding(h.linha.getPaddingLeft(), espacoTopo, h.linha.getPaddingRight(), 0);
            h.bolha.setBackground(bolha(context, true, primeira, ultima));
            h.txtTexto.setText(m.getTexto());
            h.txtHora.setText(Tempo.hora(m.getEnviadoEm()));
            if (publico) {
                h.imgStatus.setVisibility(View.GONE);
            } else {
                boolean lida = m.getEnviadoEm() <= lidoPeloContatoAte;
                h.imgStatus.setVisibility(View.VISIBLE);
                h.imgStatus.setImageResource(lida ? R.drawable.ic_done_all : R.drawable.ic_done);
                h.imgStatus.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(context,
                        lida ? R.color.read_tick : R.color.white_80)));
            }
            return;
        }

        RecebidaHolder h = (RecebidaHolder) holder;
        h.linha.setPadding(h.linha.getPaddingLeft(), espacoTopo, h.linha.getPaddingRight(), 0);
        h.bolha.setBackground(bolha(context, false, primeira, ultima));
        h.txtTexto.setText(m.getTexto());
        h.txtHora.setText(Tempo.hora(m.getEnviadoEm()));
        if (publico) {
            View.OnClickListener abrirAutor = v -> {
                if (listener != null) {
                    listener.onAutorClick(m.getUid(), m.getNome());
                }
            };
            h.txtAvatar.setVisibility(ultima ? View.VISIBLE : View.INVISIBLE);
            Avatar.aplicar(h.txtAvatar, m.getUid(), m.getNome());
            h.txtAvatar.setOnClickListener(abrirAutor);
            h.txtNome.setVisibility(primeira ? View.VISIBLE : View.GONE);
            h.txtNome.setText(m.getNome());
            h.txtNome.setTextColor(Avatar.cor(m.getUid()));
            h.txtNome.setOnClickListener(abrirAutor);
        } else {
            h.txtAvatar.setVisibility(View.GONE);
            h.txtNome.setVisibility(View.GONE);
        }
    }

    private GradientDrawable bolha(Context context, boolean enviada, boolean primeira, boolean ultima) {
        float grande = dp(context, 20);
        float pequeno = dp(context, 6);
        GradientDrawable fundo;
        if (enviada) {
            fundo = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{
                    ContextCompat.getColor(context, R.color.bubble_sent_start),
                    ContextCompat.getColor(context, R.color.bubble_sent_end)});
        } else {
            fundo = new GradientDrawable();
            fundo.setColor(ContextCompat.getColor(context, R.color.bubble_received));
        }
        // Lado "de dentro" do grupo (direita para enviadas, esquerda para recebidas) fica com canto pequeno.
        float topoDentro = primeira ? grande : pequeno;
        float baseDentro = ultima ? grande : pequeno;
        float topoEsq = enviada ? grande : topoDentro;
        float topoDir = enviada ? topoDentro : grande;
        float baseDir = enviada ? baseDentro : grande;
        float baseEsq = enviada ? grande : baseDentro;
        fundo.setCornerRadii(new float[]{topoEsq, topoEsq, topoDir, topoDir, baseDir, baseDir, baseEsq, baseEsq});
        return fundo;
    }

    private boolean mesmoGrupo(Object a, Object b) {
        if (!(a instanceof Mensagem) || !(b instanceof Mensagem)) {
            return false;
        }
        Mensagem x = (Mensagem) a;
        Mensagem y = (Mensagem) b;
        return x.getUid().equals(y.getUid())
                && Math.abs(y.getEnviadoEm() - x.getEnviadoEm()) < INTERVALO_GRUPO_MS;
    }

    private int posicaoDe(String id) {
        for (int i = itens.size() - 1; i >= 0; i--) {
            Object item = itens.get(i);
            if (item instanceof Mensagem && ((Mensagem) item).getId().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    @Nullable
    private Mensagem ultimaMensagem() {
        for (int i = itens.size() - 1; i >= 0; i--) {
            if (itens.get(i) instanceof Mensagem) {
                return (Mensagem) itens.get(i);
            }
        }
        return null;
    }

    private static int dp(Context context, float valor) {
        return (int) (valor * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    static class DataHolder extends RecyclerView.ViewHolder {
        final TextView txtData;

        DataHolder(View v) {
            super(v);
            txtData = v.findViewById(R.id.txtData);
        }
    }

    static class EnviadaHolder extends RecyclerView.ViewHolder {
        final View linha;
        final View bolha;
        final TextView txtTexto;
        final TextView txtHora;
        final ImageView imgStatus;

        EnviadaHolder(View v) {
            super(v);
            linha = v.findViewById(R.id.linhaMensagem);
            bolha = v.findViewById(R.id.bolha);
            txtTexto = v.findViewById(R.id.txtTexto);
            txtHora = v.findViewById(R.id.txtHora);
            imgStatus = v.findViewById(R.id.imgStatus);
        }
    }

    static class RecebidaHolder extends RecyclerView.ViewHolder {
        final View linha;
        final TextView txtAvatar;
        final View bolha;
        final TextView txtNome;
        final TextView txtTexto;
        final TextView txtHora;

        RecebidaHolder(View v) {
            super(v);
            linha = v.findViewById(R.id.linhaMensagem);
            txtAvatar = v.findViewById(R.id.txtAvatar);
            bolha = v.findViewById(R.id.bolha);
            txtNome = v.findViewById(R.id.txtNome);
            txtTexto = v.findViewById(R.id.txtTexto);
            txtHora = v.findViewById(R.id.txtHora);
        }
    }
}
