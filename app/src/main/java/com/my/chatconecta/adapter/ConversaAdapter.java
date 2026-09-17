package com.my.chatconecta.adapter;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.my.chatconecta.R;
import com.my.chatconecta.model.Conversa;
import com.my.chatconecta.model.Usuario;
import com.my.chatconecta.util.Avatar;
import com.my.chatconecta.util.Tempo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConversaAdapter extends RecyclerView.Adapter<ConversaAdapter.Holder> {

    public interface Listener {
        void onConversaClick(Conversa conversa, String nomeContato);

        void onConversaFotoClick(Conversa conversa, String nomeContato);
    }

    private final List<Conversa> itens = new ArrayList<>();
    private final Map<String, Usuario> usuarios = new HashMap<>();
    private final String meuUid;
    private final Listener listener;

    public ConversaAdapter(String meuUid, Listener listener) {
        this.meuUid = meuUid;
        this.listener = listener;
    }

    /** Os nomes e o status online vem do mapa de usuarios que a MainActivity ja observa. */
    public void setDados(List<Conversa> conversas, Map<String, Usuario> perfis) {
        itens.clear();
        itens.addAll(conversas);
        usuarios.clear();
        usuarios.putAll(perfis);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return itens.size();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_conversa, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        Context context = h.itemView.getContext();
        Conversa c = itens.get(position);
        Usuario contato = usuarios.get(c.getContatoUid());
        String nome = contato != null ? contato.getNome() : context.getString(R.string.usuario);

        Avatar.aplicar(h.txtAvatar, c.getContatoUid(), nome);
        h.pontoOnline.setVisibility(contato != null && contato.isOnline() ? View.VISIBLE : View.GONE);
        h.txtNome.setText(nome);
        h.txtHora.setText(Tempo.curta(context, c.getUltimaEm()));

        String ultima = c.getUltimaMensagem();
        h.txtUltima.setText(meuUid.equals(c.getUltimoUid())
                ? context.getString(R.string.voce_prefixo, ultima) : ultima);

        boolean temNovas = c.getNaoLidas() > 0;
        h.txtNaoLidas.setVisibility(temNovas ? View.VISIBLE : View.GONE);
        h.txtNaoLidas.setText(c.getNaoLidas() > 99 ? "99+" : String.valueOf(c.getNaoLidas()));
        h.txtHora.setTextColor(ContextCompat.getColor(context, temNovas ? R.color.brand : R.color.text_secondary));
        h.txtUltima.setTextColor(ContextCompat.getColor(context, temNovas ? R.color.text_primary : R.color.text_secondary));
        h.txtUltima.setTypeface(null, temNovas ? Typeface.BOLD : Typeface.NORMAL);

        h.itemView.setOnClickListener(v -> listener.onConversaClick(c, nome));
        h.areaAvatar.setOnClickListener(v -> listener.onConversaFotoClick(c, nome));
    }

    static class Holder extends RecyclerView.ViewHolder {
        final View areaAvatar;
        final TextView txtAvatar;
        final View pontoOnline;
        final TextView txtNome;
        final TextView txtHora;
        final TextView txtUltima;
        final TextView txtNaoLidas;

        Holder(View v) {
            super(v);
            areaAvatar = v.findViewById(R.id.areaAvatar);
            txtAvatar = v.findViewById(R.id.txtAvatar);
            pontoOnline = v.findViewById(R.id.pontoOnline);
            txtNome = v.findViewById(R.id.txtNome);
            txtHora = v.findViewById(R.id.txtHora);
            txtUltima = v.findViewById(R.id.txtUltima);
            txtNaoLidas = v.findViewById(R.id.txtNaoLidas);
        }
    }
}
