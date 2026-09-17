package com.my.chatconecta.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.my.chatconecta.R;
import com.my.chatconecta.model.Usuario;
import com.my.chatconecta.util.Avatar;
import com.my.chatconecta.util.Tempo;

import java.util.ArrayList;
import java.util.List;

/** Lista de perfis. compacto = faixa horizontal no topo do chat publico. */
public class PessoaAdapter extends RecyclerView.Adapter<PessoaAdapter.Holder> {

    public interface Listener {
        /** Toque na foto/linha: abre a folha de perfil com a opcao "Abrir chat". */
        void onPessoaClick(Usuario usuario);

        /** Toque no icone de balao da lista: vai direto para o chat. */
        void onConversarClick(Usuario usuario);
    }

    private final List<Usuario> itens = new ArrayList<>();
    private final boolean compacto;
    private final Listener listener;

    public PessoaAdapter(boolean compacto, Listener listener) {
        this.compacto = compacto;
        this.listener = listener;
    }

    public void setItens(List<Usuario> novos) {
        itens.clear();
        itens.addAll(novos);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return itens.size();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = compacto ? R.layout.item_pessoa_mini : R.layout.item_pessoa;
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(layout, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        Usuario u = itens.get(position);
        Context context = h.itemView.getContext();
        Avatar.aplicar(h.txtAvatar, u.getUid(), u.getNome());
        h.pontoOnline.setVisibility(u.isOnline() ? View.VISIBLE : View.GONE);
        h.txtNome.setText(compacto ? Avatar.primeiroNome(u.getNome()) : u.getNome());
        h.itemView.setOnClickListener(v -> listener.onPessoaClick(u));

        if (h.txtDetalhe != null) {
            h.txtDetalhe.setText(u.getRecado().isEmpty() ? Tempo.status(context, u) : u.getRecado());
        }
        if (h.icoConversar != null) {
            h.icoConversar.setOnClickListener(v -> listener.onConversarClick(u));
        }
    }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView txtAvatar;
        final View pontoOnline;
        final TextView txtNome;
        final TextView txtDetalhe;
        final View icoConversar;

        Holder(View v) {
            super(v);
            txtAvatar = v.findViewById(R.id.txtAvatar);
            pontoOnline = v.findViewById(R.id.pontoOnline);
            txtNome = v.findViewById(R.id.txtNome);
            txtDetalhe = v.findViewById(R.id.txtDetalhe);
            icoConversar = v.findViewById(R.id.icoConversar);
        }
    }
}
