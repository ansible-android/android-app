package org.ansible.asnet;

public interface RequestDelegate {
    void run(TLObject response, TLRPC.TL_error error);
}
