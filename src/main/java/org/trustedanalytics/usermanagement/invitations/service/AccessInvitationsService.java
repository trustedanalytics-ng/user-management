/**
 *  Copyright (c) 2015 Intel Corporation 
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.trustedanalytics.usermanagement.invitations.service;

import com.google.common.base.Strings;
import org.trustedanalytics.usermanagement.storage.KeyValueStore;

import java.util.Optional;
import java.util.function.Consumer;

public class AccessInvitationsService {

    public enum CreateOrUpdateState {
        CREATED,
        UPDATED
    }

    private final KeyValueStore<AccessInvitations> store;

    public AccessInvitationsService(KeyValueStore<AccessInvitations> store){
        this.store = store;
    }

    public Optional<AccessInvitations> getAccessInvitations(String email) {
        validateStringArgument(email);
        return Optional.ofNullable(store.get(email));
    }

    public void updateAccessInvitation(String email, AccessInvitations invitations) {
        validateStringArgument(email);
        store.put(email, invitations);
    }

    public void redeemAccessInvitations(String email) {
        validateStringArgument(email);
        store.remove(email);
    }

    public CreateOrUpdateState createOrUpdateInvitation(String email, Consumer<AccessInvitations> consumer) {
        validateStringArgument(email);
        AccessInvitations userInvitations;
        CreateOrUpdateState state;

        if (store.hasKey(email)) {
            userInvitations = store.get(email);
            state = CreateOrUpdateState.UPDATED;
        } else {
            userInvitations = new AccessInvitations();
            state = CreateOrUpdateState.CREATED;
        }

        consumer.accept(userInvitations);
        store.put(email, userInvitations);
        return state;
    }

    private void validateStringArgument(String arg) {
        if(Strings.isNullOrEmpty(arg)) {
            throw new IllegalArgumentException("String argument is null or empty");
        }
    }
}
